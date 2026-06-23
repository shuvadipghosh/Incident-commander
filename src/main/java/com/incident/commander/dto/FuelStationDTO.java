package com.incident.commander.dto;

import lombok.Data;

@Data
public class FuelStationDTO {
    private String name;
    private String street;
    private double latitude;
    private double longitude;
    private double distanceKm;
}
