package com.incident.commander.agent.tools;

import com.incident.commander.dto.WeatherSummaryDTO;
import com.incident.commander.Service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeatherTools {

    private final WeatherService weatherService;

    @Tool(description = "Get current weather conditions (temperature, rain level, weather description) for the given coordinates. Always call this to assess safety conditions.")
    public WeatherSummaryDTO getCurrentWeather(double latitude, double longitude) {
        try {
            return weatherService.getCurrentWeather(latitude, longitude);
        } catch (Exception e) {
            WeatherSummaryDTO fallback = new WeatherSummaryDTO();
            fallback.setCondition("Unknown");
            fallback.setTemperature(20.0);
            fallback.setRain(0.0);
            return fallback;
        }
    }
}
