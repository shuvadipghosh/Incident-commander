package com.incident.commander.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IncidentContext {
    private IncidentType incidentType;
    private String description;
    private double latitude;
    private double longitude;
    private long phoneNumber;
    private String sessionId;
}
