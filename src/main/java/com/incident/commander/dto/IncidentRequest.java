package com.incident.commander.dto;

import com.incident.commander.domain.IncidentType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IncidentRequest {
    @NotBlank(message = "Description is required")
    private String description;
    private String scenario; // kept for backward compat with AiService
    private IncidentType incidentTypeHint; // optional UI hint for classifier
    private double latitude;
    private double longitude;
    private long phoneNumber;
    private String sessionId;
}
