package com.incident.commander.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class IncidentResult {
    private IncidentType incidentType;
    private String summary;
    private WeatherContext weather;
    private List<RecommendedAction> recommendations;
    private Map<String, Object> contextData; // flexible map for type-specific data (distance, hospital name, etc.)
    private DispatchDetails dispatchDetails; // details of any service dispatched by the LLM
    private String sessionId;

    @Data
    @Builder
    public static class WeatherContext {
        private String condition;
        private double temperature;
        private double rain;
    }

    @Data
    @Builder
    public static class RecommendedAction {
        private int rank;
        private String action;
        private String reason;
        private String eta;
    }

    @Data
    @Builder
    public static class DispatchDetails {
        private String status;
        private String serviceType;
        private String provider;
        private String confirmationId;
        private String eta;
    }
}
