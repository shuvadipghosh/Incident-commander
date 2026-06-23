package com.incident.commander.agent.tools;

import com.incident.commander.dto.FuelStationDTO;
import com.incident.commander.Service.OverpassService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HospitalTools {

    private final OverpassService overpassService;

    @Tool(description = "Find the nearest hospital or emergency medical facility to the given coordinates. Returns name, street, distance in km.")
    public FuelStationDTO findNearestHospital(double latitude, double longitude) {
        try {
            return overpassService.findNearestHospital(latitude, longitude);
        } catch (Exception e) {
            FuelStationDTO fallback = new FuelStationDTO();
            fallback.setName("Unable to locate nearby hospital");
            fallback.setDistanceKm(-1);
            return fallback;
        }
    }
}
