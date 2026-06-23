package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentType;
import org.springframework.stereotype.Component;

@Component
public class AccidentHandler extends BaseIncidentHandler {
    public AccidentHandler(IncidentAgent incidentAgent) {
        super(incidentAgent);
    }

    @Override
    public IncidentType supports() {
        return IncidentType.ACCIDENT;
    }
}
