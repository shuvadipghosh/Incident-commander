package com.incident.commander.dto;

public class WeatherResponseDTO {
    private CurrentWeatherDTO current;

    public CurrentWeatherDTO getCurrent() {
        return current;
    }

    public void setCurrent(CurrentWeatherDTO current) {
        this.current = current;
    }
}
