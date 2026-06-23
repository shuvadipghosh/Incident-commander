package com.incident.commander.dto;

import lombok.Data;

@Data
public class WeatherSummaryDTO {
    private double temperature;
    private double rain;
    private String condition;
}