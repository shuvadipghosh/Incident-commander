package com.incident.commander.handler;

import com.incident.commander.agent.IncidentAgent;
import com.incident.commander.domain.IncidentType;
import org.springframework.stereotype.Component;

@Component
public class NearbyMechanicHandler extends BaseIncidentHandler {
    public NearbyMechanicHandler(IncidentAgent incidentAgent) {
        super(incidentAgent);
    }

    @Override
    public IncidentType supports() {
        return IncidentType.NEARBY_MECHANIC;
    }
}
