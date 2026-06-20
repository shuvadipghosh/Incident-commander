package com.incident.commander.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CurrentWeatherDTO {

    @JsonProperty("temperature_2m")
    private double temperature;

    private double precipitation;

    private double rain;

    @JsonProperty("weather_code")
    private int weatherCode;

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }

    public double getRain() {
        return rain;
    }

    public void setRain(double rain) {
        this.rain = rain;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
    }
}