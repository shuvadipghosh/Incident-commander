package com.incident.commander.MCP.MCPController;

import com.incident.commander.Service.OverpassService;
import com.incident.commander.dto.FuelStationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class LocationController {

    @Autowired
    OverpassService overpassService;
    @Tool(description = "Find nearest fuel station")
    public FuelStationDTO findNearestFuelStations(
            double latitude,
            double longitude) {

        FuelStationDTO fuelStationDTO = new FuelStationDTO();

        try {
             fuelStationDTO = overpassService.findNearestFuelStation(latitude, longitude);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return fuelStationDTO;
    }

    @GetMapping("/fuel")
    public ResponseEntity<Object> fuel(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        FuelStationDTO fuelStationDTO = new FuelStationDTO();

        try {
            fuelStationDTO = overpassService.findNearestFuelStation(latitude, longitude);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
        return ResponseEntity.ok(fuelStationDTO);
    }
}
