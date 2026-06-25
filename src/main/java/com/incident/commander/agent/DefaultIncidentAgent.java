package com.incident.commander.agent;

import com.incident.commander.agent.tools.HospitalTools;
import com.incident.commander.agent.tools.LocationTools;
import com.incident.commander.agent.tools.MechanicTools;
import com.incident.commander.agent.tools.TowTools;
import com.incident.commander.agent.tools.WeatherTools;
import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import com.incident.commander.domain.IncidentType;
import com.incident.commander.knowledge.KnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DefaultIncidentAgent implements IncidentAgent {

    private static final Logger log = LoggerFactory.getLogger(DefaultIncidentAgent.class);

    private final ChatModel chatModel;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;
    private final LocationTools locationTools;
    private final WeatherTools weatherTools;
    private final MechanicTools mechanicTools;
    private final HospitalTools hospitalTools;
    private final TowTools towTools;

    public DefaultIncidentAgent(ChatModel chatModel,
                                 KnowledgeService knowledgeService,
                                 ObjectMapper objectMapper,
                                 LocationTools locationTools,
                                 WeatherTools weatherTools,
                                 MechanicTools mechanicTools,
                                 HospitalTools hospitalTools,
                                 TowTools towTools) {
        this.chatModel = chatModel;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
        this.locationTools = locationTools;
        this.weatherTools = weatherTools;
        this.mechanicTools = mechanicTools;
        this.hospitalTools = hospitalTools;
        this.towTools = towTools;
    }

    @Override
    public IncidentResult analyze(IncidentContext context) {
        IncidentType type = context.getIncidentType();

        // Load per-incident system prompt
        String systemPrompt = loadPrompt(type);

        // Retrieve relevant RAG context
        List<String> relevantRules = knowledgeService.retrieveRelevantRules(type, context.getDescription());
        if (!relevantRules.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n## Relevant Policy Rules\n" + String.join("\n", relevantRules);
        }

        // Construct detailed prompt snippet outlining user's coverage
        StringBuilder coveragePrompt = new StringBuilder();
        coveragePrompt.append("\n\n## User Insurance Coverage\n");
        if (context.isInsured()) {
            coveragePrompt.append("Policy Number: ").append(context.getPolicyNumber()).append("\n");
            coveragePrompt.append("Policy Holder: ").append(context.getPolicyHolder()).append("\n");
            coveragePrompt.append("Covered Services/Actions: ");
            if (context.getCoverage() != null && !context.getCoverage().isEmpty()) {
                coveragePrompt.append(String.join(", ", context.getCoverage()));
            } else {
                coveragePrompt.append("None");
            }
            coveragePrompt.append("\n");
        } else {
            coveragePrompt.append("The user is uninsured (no active policy or policy not found).\n");
        }

        // Append cost calculation instructions to systemPrompt
        coveragePrompt.append("\n## Cost Calculation Instructions\n")
                .append("You must dynamically calculate the cost for each recommended action and dispatch details in the returned JSON.\n")
                .append("1. Look at the 'User Insurance Coverage' above to determine what services/coverage the user has.\n")
                .append("2. Look at the 'Relevant Policy Rules' (which contains rates from service-providers.md) to find the standard price for the service/action (e.g., $75 for fuel delivery, $150 for towing/tow truck, $80 for tyre repair/flat tyre, $80 for battery jump start/dead battery, etc.).\n")
                .append("3. If the service or action is covered by the user's insurance coverage, set the 'cost' field to exactly \"Covered\".\n")
                .append("4. If the user is uninsured or the service/action is not covered, set the 'cost' field to the price specified in the rules (e.g., \"$75\", \"$150\", \"$80\", etc.). If no price is specified, you can set it to null or calculate/omit as appropriate.\n")
                .append("Apply this cost calculation logic to both the 'cost' field of each object in the 'recommendations' array and the 'cost' field of the 'dispatchDetails' object. For recommended actions, match the action name (e.g., 'FUEL_DELIVERY', 'TOW_TRUCK', 'TYRE_REPAIR_SERVICE', 'JUMP_START_SERVICE', etc.) to the coverage/rules. For dispatch details, match the service type. Make sure to include the 'cost' field in the returned JSON matching this calculation.\n");

        systemPrompt = systemPrompt + coveragePrompt.toString();

        // Select tools for this incident type
        Object[] tools = selectTools(type);

        // Build ChatClient with appropriate tools
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(tools)
                .build();

        String userMessage = buildUserMessage(context);

        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            log.debug("Raw LLM response for {}: {}", type, response);
            return parseResponse(response, context);

        } catch (Exception e) {
            log.error("Error analyzing incident {} for session {}", type, context.getSessionId(), e);
            return IncidentResult.builder()
                    .incidentType(type)
                    .summary("We are processing your request. A specialist will assist you shortly.")
                    .recommendations(List.of(
                            IncidentResult.RecommendedAction.builder()
                                    .rank(1).action("CONTACT_SUPPORT")
                                    .reason("Automated processing encountered an issue.")
                                    .build()
                    ))
                    .sessionId(context.getSessionId())
                    .build();
        }
    }

    private Object[] selectTools(IncidentType type) {
        return switch (type) {
            case OUT_OF_FUEL -> new Object[]{locationTools, weatherTools, towTools};
            case FLAT_TYRE -> new Object[]{mechanicTools, weatherTools, towTools};
            case DEAD_BATTERY -> new Object[]{mechanicTools, weatherTools, towTools};
            case VEHICLE_SMOKE -> new Object[]{towTools, weatherTools};
            case MEDICAL_EMERGENCY -> new Object[]{hospitalTools};
            case ACCIDENT -> new Object[]{hospitalTools, towTools};
            case NEARBY_MECHANIC -> new Object[]{mechanicTools};
            case TOW_REQUEST -> new Object[]{towTools, weatherTools};
            default -> new Object[]{weatherTools};
        };
    }

    private String buildUserMessage(IncidentContext context) {
        return """
                Incident Type: %s
                User Description: %s
                Location: lat=%s, lon=%s
                Session ID: %s
                """.formatted(
                context.getIncidentType(),
                context.getDescription(),
                context.getLatitude(),
                context.getLongitude(),
                context.getSessionId()
        );
    }

    private String loadPrompt(IncidentType type) {
        String filename = "prompts/" + type.name().toLowerCase() + ".md";
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Could not load prompt file: {}", filename);
        }
        // Fallback to unknown prompt
        try {
            ClassPathResource fallbackResource = new ClassPathResource("prompts/unknown.md");
            if (fallbackResource.exists()) {
                return new String(fallbackResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Could not load fallback prompt file");
        }
        return "You are an AI Incident Commander. Help the customer with their roadside emergency. Return valid JSON.";
    }

    private IncidentResult parseResponse(String response, IncidentContext context) {
        String cleaned = response.trim();

        // 1. Try to find the markdown code block ```json ... ```
        int jsonStartIndex = cleaned.indexOf("```json");
        if (jsonStartIndex != -1) {
            int contentStart = jsonStartIndex + 7;
            int jsonEndIndex = cleaned.indexOf("```", contentStart);
            if (jsonEndIndex != -1) {
                cleaned = cleaned.substring(contentStart, jsonEndIndex).trim();
            } else {
                cleaned = cleaned.substring(contentStart).trim();
            }
        } else {
            // 2. Try to find generic ``` ... ```
            int codeStartIndex = cleaned.indexOf("```");
            if (codeStartIndex != -1) {
                int contentStart = codeStartIndex + 3;
                int codeEndIndex = cleaned.indexOf("```", contentStart);
                if (codeEndIndex != -1) {
                    cleaned = cleaned.substring(contentStart, codeEndIndex).trim();
                } else {
                    cleaned = cleaned.substring(contentStart).trim();
                }
            }
        }

        // 3. Fallback: find the first '{' and last '}'
        if (!cleaned.startsWith("{")) {
            int firstBrace = cleaned.indexOf("{");
            int lastBrace = cleaned.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1).trim();
            }
        }

        try {
            IncidentResult result = objectMapper.readValue(cleaned, IncidentResult.class);
            if (result.getIncidentType() == null) result.setIncidentType(context.getIncidentType());
            if (result.getSessionId() == null) result.setSessionId(context.getSessionId());
            return result;
        } catch (Exception e) {
            log.warn("Could not parse LLM JSON response, building fallback result. Error: {}", e.getMessage());
            return IncidentResult.builder()
                    .incidentType(context.getIncidentType())
                    .summary(cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned)
                    .sessionId(context.getSessionId())
                    .recommendations(List.of(
                            IncidentResult.RecommendedAction.builder()
                                    .rank(1).action("CONTACT_SUPPORT")
                                    .reason("Please stay safe. A support agent will assist you.")
                                    .build()
                    ))
                    .build();
        }
    }
}
