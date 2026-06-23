package com.incident.commander.agent;

import com.incident.commander.domain.IncidentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class ClassifierAgent {

    private static final Logger log = LoggerFactory.getLogger(ClassifierAgent.class);

    private static final String CLASSIFIER_SYSTEM_PROMPT = """
            You are an incident classification system for a roadside assistance platform.
            
            Given a user's description of their problem, classify it into EXACTLY ONE of these incident types:
            OUT_OF_FUEL, FLAT_TYRE, DEAD_BATTERY, VEHICLE_SMOKE, MEDICAL_EMERGENCY, ACCIDENT, NEARBY_MECHANIC, TOW_REQUEST, UNKNOWN
            
            Rules:
            - VEHICLE_SMOKE: any smoke, fire, burning smell, overheating
            - MEDICAL_EMERGENCY: injury, chest pain, unconscious, bleeding, medical help needed
            - ACCIDENT: collision, crash, hit, struck
            - DEAD_BATTERY: battery dead, won't start, jump start needed, no power
            - FLAT_TYRE: flat tire/tyre, puncture, blown tyre, need spare
            - OUT_OF_FUEL: no fuel, ran out of gas/petrol/diesel
            - NEARBY_MECHANIC: looking for mechanic, car repair shop
            - TOW_REQUEST: need tow, tow truck, vehicle needs towing
            - UNKNOWN: cannot determine from description
            
            If a UI hint is provided, use it as a strong signal but the description overrides it if contradictory.
            
            Respond with ONLY the enum value, nothing else. Example: FLAT_TYRE
            """;

    private final ChatClient classifierClient;

    public ClassifierAgent(ChatModel chatModel) {
        this.classifierClient = ChatClient.builder(chatModel).build();
    }

    public IncidentType classify(String description, IncidentType hint) {
        String hintText = (hint != null && hint != IncidentType.UNKNOWN)
                ? "\nUI hint from user: " + hint.name()
                : "";

        String userMessage = "User description: " + description + hintText;

        try {
            String response = classifierClient.prompt()
                    .system(CLASSIFIER_SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();

            String normalized = response.trim().toUpperCase().replaceAll("[^A-Z_]", "");
            IncidentType classified = IncidentType.valueOf(normalized);
            log.info("Classified '{}' as {} (hint was: {})", description, classified, hint);
            return classified;

        } catch (IllegalArgumentException e) {
            log.warn("Could not parse classification response, falling back to hint or UNKNOWN. hint={}", hint);
            return (hint != null) ? hint : IncidentType.UNKNOWN;
        } catch (Exception e) {
            log.error("Classification failed", e);
            return (hint != null) ? hint : IncidentType.UNKNOWN;
        }
    }
}
