package com.incident.commander.MCP.MCPController;


import com.incident.commander.Service.WeatherService;
import com.incident.commander.dto.WeatherSummaryDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class WeatherController {

    @Autowired
    WeatherService weatherService;

        @Tool(description = "Get current weather information for a specific location")
        public WeatherSummaryDTO getCurrentWeather(double latitude, double longitude) {
            try {
                return weatherService.getCurrentWeather(latitude, longitude);
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve weather information: " + e.getMessage(), e);
            }
        }


    @GetMapping("/weather")
    public ResponseEntity<WeatherSummaryDTO> weather(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        return ResponseEntity.ok(weatherService.getCurrentWeather(
                latitude,
                longitude));
    }
}
