package com.incident.commander.agent.tools;

import com.incident.commander.dto.FuelStationDTO;
import com.incident.commander.Service.OverpassService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationTools {

    private final OverpassService overpassService;

    @Tool(description = "Find the nearest fuel/petrol station to the given coordinates. Returns name, street, distance in km, and coordinates.")
    public FuelStationDTO findNearestFuelStation(double latitude, double longitude) {
        try {
            return overpassService.findNearestFuelStation(latitude, longitude);
        } catch (Exception e) {
            FuelStationDTO fallback = new FuelStationDTO();
            fallback.setName("Unable to locate fuel station");
            fallback.setDistanceKm(-1);
            return fallback;
        }
    }
}
