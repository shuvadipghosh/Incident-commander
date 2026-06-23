package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentType;
import org.springframework.stereotype.Component;

@Component
public class DeadBatteryHandler extends BaseIncidentHandler {
    public DeadBatteryHandler(IncidentAgent incidentAgent) {
        super(incidentAgent);
    }

    @Override
    public IncidentType supports() {
        return IncidentType.DEAD_BATTERY;
    }
}
