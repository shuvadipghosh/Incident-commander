package com.incident.commander.dto;

import java.util.List;

public class IncidentResponseDTO {

    private String summary;

    private WeatherSummaryDTO weather;

    private List<RecommendationDTO> recommendations;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public WeatherSummaryDTO getWeather() {
        return weather;
    }

    public void setWeather(WeatherSummaryDTO weather) {
        this.weather = weather;
    }

    public List<RecommendationDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationDTO> recommendations) {
        this.recommendations = recommendations;
    }
}
