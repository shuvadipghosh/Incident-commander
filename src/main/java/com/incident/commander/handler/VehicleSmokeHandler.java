package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentContext;
import com.incident.commander.domain.IncidentResult;
import com.incident.commander.domain.IncidentType;
import org.springframework.stereotype.Component;

@Component
public class VehicleSmokeHandler extends BaseIncidentHandler {
    public VehicleSmokeHandler(IncidentAgent incidentAgent) {
        super(incidentAgent);
    }

    @Override
    public IncidentType supports() {
        return IncidentType.VEHICLE_SMOKE;
    }

    @Override
    public IncidentResult handle(IncidentContext context) {
        // Safety override: always prepend a safety warning before LLM analysis
        log.warn("SAFETY: Vehicle smoke incident detected for session {}", context.getSessionId());
        IncidentResult result = super.handle(context);
        // Ensure safety alert is prepended to summary for smoke incidents
        if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
            result.setSummary("\u26A0\uFE0F SAFETY ALERT: " + result.getSummary());
        }
        return result;
    }
}
