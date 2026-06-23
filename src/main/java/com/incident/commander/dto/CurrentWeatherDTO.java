package com.incident.commander.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CurrentWeatherDTO {

    @JsonProperty("temperature_2m")
    private double temperature;

    private double precipitation;

    private double rain;

    @JsonProperty("weather_code")
    private int weatherCode;
}