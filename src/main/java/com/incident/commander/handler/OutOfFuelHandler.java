package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentType;
import org.springframework.stereotype.Component;

@Component
public class OutOfFuelHandler extends BaseIncidentHandler {
    public OutOfFuelHandler(IncidentAgent incidentAgent) {
        super(incidentAgent);
    }

    @Override
    public IncidentType supports() {
        return IncidentType.OUT_OF_FUEL;
    }
}
