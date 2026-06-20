package com.incident.commander.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.commander.MCP.MCPController.LocationController;
import com.incident.commander.Util.DistanceUtil;
import com.incident.commander.dto.FuelStationDTO;
import com.incident.commander.dto.OverPassResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;

@Service

public class OverpassService {
    private static final Logger log =
            LoggerFactory.getLogger(OverpassService.class);
    @Autowired
    RestClient restClient;
    @Autowired
    ObjectMapper objectMapper;

    public FuelStationDTO findNearestFuelStation(
            double userLat,
            double userLon) throws Exception {

        String query = String.format("""
                [out:json];
                node["amenity"="fuel"]
                (around:5000,%s,%s);
                out;
                """, userLat, userLon);

        String response = restClient.get()
                .uri("https://overpass-api.de/api/interpreter?data={query}",
                        query)
                .retrieve()
                .body(String.class);

        log.info("Overpass API Response: {}", response);

        OverPassResponseDTO overpassResponse =
                objectMapper.readValue(response, OverPassResponseDTO.class);

        if(overpassResponse.getElements() == null || overpassResponse.getElements().isEmpty()) {
            throw new RuntimeException("No fuel stations found");
        }

        return overpassResponse.getElements()
                .stream()
                .map(element -> {

                    String name =
                            element.getTags().getOrDefault(
                                    "name",
                                    "Unknown Fuel Station");

                    String street =
                            element.getTags().getOrDefault(
                                    "addr:street",
                                    "Unknown");

                    double distance =
                            DistanceUtil.distance(
                                    userLat,
                                    userLon,
                                    element.getLat(),
                                    element.getLon());

                    FuelStationDTO station = new FuelStationDTO();

                    station.setName(name);
                    station.setStreet(street);
                    station.setLatitude(element.getLat());
                    station.setLongitude(element.getLon());
                    station.setDistanceKm(distance);

                    return station;
                })
                .min(Comparator.comparing(
                        FuelStationDTO::getDistanceKm))
                .orElseThrow(() ->
                        new RuntimeException("No fuel stations found"));
    }
}