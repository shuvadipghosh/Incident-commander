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

}
