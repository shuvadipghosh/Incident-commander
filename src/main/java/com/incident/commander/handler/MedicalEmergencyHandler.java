package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import com.incident.commander.domain.IncidentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MedicalEmergencyHandler extends BaseIncidentHandler {
    public MedicalEmergencyHandler(IncidentAgent incidentAgent) {
        super(incidentAgent);
    }

    @Override
    public IncidentType supports() {
        return IncidentType.MEDICAL_EMERGENCY;
    }

    @Override
    public IncidentResult handle(IncidentContext context) {
        log.warn("MEDICAL EMERGENCY for session {}", context.getSessionId());
        IncidentResult result = super.handle(context);
        // Always prepend 911 call action for medical emergencies
        List<IncidentResult.RecommendedAction> actions = new ArrayList<>();
        actions.add(IncidentResult.RecommendedAction.builder()
                .rank(0)
                .action("CALL_911")
                .reason("Medical emergency — call emergency services immediately if life is at risk.")
                .build());
        if (result.getRecommendations() != null) {
            actions.addAll(result.getRecommendations());
        }
        result.setRecommendations(actions);
        return result;
    }
}
