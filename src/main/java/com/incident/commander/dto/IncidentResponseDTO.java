package com.incident.commander.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class IncidentResponseDTO {
    private String incidentType;
    private String summary;
    private WeatherDTO weather;
    private double nearestFuelPump; // kept for OUT_OF_FUEL backward compat
    private List<RecommendationDTO> recommendations;
    private Map<String, Object> contextData;
    private DispatchDetailsDTO dispatchDetails;
    private String sessionId;

    @Data
    public static class WeatherDTO {
        private String condition;
        private double temperature;
        private double rain;
    }

    @Data
    public static class DispatchDetailsDTO {
        private String status;
        private String serviceType;
        private String provider;
        private String confirmationId;
        private String eta;
        private String cost;
    }
}
