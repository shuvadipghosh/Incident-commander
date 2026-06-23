package com.incident.commander.agent.tools;

import com.incident.commander.dto.FuelStationDTO;
import com.incident.commander.Service.OverpassService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MechanicTools {

    private final OverpassService overpassService;

    @Tool(description = "Find the nearest vehicle repair mechanic or tyre shop to the given coordinates. Returns name, street, distance in km.")
    public FuelStationDTO findNearestMechanic(double latitude, double longitude) {
        try {
            return overpassService.findNearestMechanic(latitude, longitude);
        } catch (Exception e) {
            FuelStationDTO fallback = new FuelStationDTO();
            fallback.setName("Unable to locate nearby mechanic");
            fallback.setDistanceKm(-1);
            return fallback;
        }
    }
}
