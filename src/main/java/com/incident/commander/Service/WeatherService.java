package com.incident.commander.Service;


import com.incident.commander.Util.WeatherUtil;
import com.incident.commander.dto.WeatherResponseDTO;
import com.incident.commander.dto.WeatherSummaryDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private final RestClient restClient;

    public WeatherService(RestClient restClient) {
        this.restClient = restClient;
    }

    public WeatherSummaryDTO getCurrentWeather(
            double latitude,
            double longitude) {

        WeatherResponseDTO response =
                restClient.get()
                        .uri(
                                "https://api.open-meteo.com/v1/forecast" +
                                        "?latitude={lat}" +
                                        "&longitude={lon}" +
                                        "&current=temperature_2m,precipitation,rain,weather_code",
                                latitude,
                                longitude)
                        .retrieve()
                        .body(WeatherResponseDTO.class);

        WeatherSummaryDTO summary =
                new WeatherSummaryDTO();

        summary.setTemperature(
                response.getCurrent().getTemperature());

        summary.setRain(
                response.getCurrent().getRain());

        summary.setCondition(
                WeatherUtil.getCondition(
                        response.getCurrent().getWeatherCode()));

        return summary;
    }
}