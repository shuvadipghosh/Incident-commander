package com.incident.commander.controller;

import com.incident.commander.agent.ClassifierAgent;
import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import com.incident.commander.domain.IncidentType;
import com.incident.commander.dto.IncidentRequest;
import com.incident.commander.dto.IncidentResponseDTO;
import com.incident.commander.dto.RecommendationDTO;
import com.incident.commander.pipeline.IncidentPipeline;
import com.incident.commander.service.PolicyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/incident")
public class IncidentController {

    private static final Logger log = LoggerFactory.getLogger(IncidentController.class);

    private final IncidentPipeline incidentPipeline;
    private final ClassifierAgent classifierAgent;
    private final PolicyService policyService;
    private final SimpMessagingTemplate messagingTemplate;

    public IncidentController(IncidentPipeline incidentPipeline,
                              ClassifierAgent classifierAgent,
                              PolicyService policyService,
                              SimpMessagingTemplate messagingTemplate) {
        this.incidentPipeline = incidentPipeline;
        this.classifierAgent = classifierAgent;
        this.policyService = policyService;
        this.messagingTemplate = messagingTemplate;
    }

    // REST fallback endpoint (synchronous)
    @PostMapping("/analyze")
    public ResponseEntity<IncidentResponseDTO> analyze(@Valid @RequestBody IncidentRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        IncidentResult result = processIncident(request, sessionId);
        return ResponseEntity.ok(toResponseDTO(result, request.getPhoneNumber()));
    }

    // WebSocket STOMP endpoint (async with progress events)
    @MessageMapping("/incident.analyze")
    public void analyzeViaWebSocket(IncidentRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        String topic = "/topic/incident/" + sessionId;

        // Step 1: Notify classifying
        sendProgress(topic, "CLASSIFYING", null, null, sessionId);

        // Step 2: Classify
        IncidentType type = classifierAgent.classify(request.getDescription(), request.getIncidentTypeHint());
        sendProgress(topic, "CLASSIFIED", type.name(), null, sessionId);

        // Step 3: Process (tools will be called inside the agent)
        try {
            IncidentContext context = IncidentContext.builder()
                    .incidentType(type)
                    .description(request.getDescription())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .phoneNumber(request.getPhoneNumber())
                    .sessionId(sessionId)
                    .build();

            IncidentResult result = incidentPipeline.process(context);

            // Step 4: Send final result
            Map<String, Object> completePayload = new HashMap<>();
            completePayload.put("status", "COMPLETE");
            completePayload.put("sessionId", sessionId);
            completePayload.put("result", toResponseDTO(result, request.getPhoneNumber()));
            messagingTemplate.convertAndSend(topic, completePayload);

        } catch (Exception e) {
            log.error("Error processing WebSocket incident for session {}", sessionId, e);
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("status", "ERROR");
            errorPayload.put("sessionId", sessionId);
            errorPayload.put("message", e.getMessage());
            messagingTemplate.convertAndSend(topic, errorPayload);
        }
    }

    private IncidentResult processIncident(IncidentRequest request, String sessionId) {
        IncidentType type = classifierAgent.classify(request.getDescription(), request.getIncidentTypeHint());
        log.info("Incident classified as {} for session {}", type, sessionId);

        IncidentContext context = IncidentContext.builder()
                .incidentType(type)
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phoneNumber(request.getPhoneNumber())
                .sessionId(sessionId)
                .build();

        return incidentPipeline.process(context);
    }

    private void sendProgress(String topic, String status, String type, String tool, String sessionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("sessionId", sessionId);
        if (type != null) payload.put("type", type);
        if (tool != null) payload.put("tool", tool);
        messagingTemplate.convertAndSend(topic, payload);
    }

    private IncidentResponseDTO toResponseDTO(IncidentResult result, long phoneNumber) {
        IncidentResponseDTO dto = new IncidentResponseDTO();
        dto.setIncidentType(result.getIncidentType() != null ? result.getIncidentType().name() : null);
        dto.setSummary(result.getSummary());
        dto.setContextData(result.getContextData());
        dto.setSessionId(result.getSessionId());

        if (result.getWeather() != null) {
            IncidentResponseDTO.WeatherDTO weatherDTO = new IncidentResponseDTO.WeatherDTO();
            weatherDTO.setCondition(result.getWeather().getCondition());
            weatherDTO.setTemperature(result.getWeather().getTemperature());
            weatherDTO.setRain(result.getWeather().getRain());
            dto.setWeather(weatherDTO);
        }

        if (result.getRecommendations() != null) {
            dto.setRecommendations(result.getRecommendations().stream()
                    .map(a -> {
                        RecommendationDTO r = new RecommendationDTO();
                        r.setRank(a.getRank());
                        r.setAction(a.getAction());
                        r.setReason(a.getReason());
                        r.setEta(a.getEta());
                        return r;
                    })
                    .toList());
        }

        if (result.getDispatchDetails() != null) {
            IncidentResponseDTO.DispatchDetailsDTO ddDTO = new IncidentResponseDTO.DispatchDetailsDTO();
            ddDTO.setStatus(result.getDispatchDetails().getStatus());
            ddDTO.setServiceType(result.getDispatchDetails().getServiceType());
            ddDTO.setProvider(result.getDispatchDetails().getProvider());
            ddDTO.setConfirmationId(result.getDispatchDetails().getConfirmationId());
            ddDTO.setEta(result.getDispatchDetails().getEta());
            dto.setDispatchDetails(ddDTO);
        }

        // Attach policy coverage info
        policyService.findByPhoneNumber(phoneNumber).ifPresent(policy -> {
            Map<String, Object> ctx = dto.getContextData() != null ? new HashMap<>(dto.getContextData()) : new HashMap<>();
            ctx.put("policyHolder", policy.getName());
            ctx.put("policyNumber", policy.getPolicyNumber());
            ctx.put("coverage", policy.getCoverage());
            dto.setContextData(ctx);
        });

        return dto;
    }
}
