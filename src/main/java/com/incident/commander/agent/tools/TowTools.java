package com.incident.commander.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TowTools {

    @Tool(description = "Dispatch a tow truck to the given coordinates. Returns a confirmation ID and estimated arrival time in minutes.")
    public Map<String, Object> dispatchTowTruck(double latitude, double longitude, String vehicleDescription) {
        // Mock tow truck dispatch
        String confirmationId = "TOW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int etaMinutes = ThreadLocalRandom.current().nextInt(20, 45);

        return Map.of(
                "confirmationId", confirmationId,
                "etaMinutes", etaMinutes,
                "provider", "Allstate Roadside Tow Assist",
                "status", "DISPATCHED",
                "message", "A tow truck has been dispatched. Confirmation: " + confirmationId
        );
    }

    @Tool(description = "Dispatch fuel delivery service (2 gallons of fuel) to the given coordinates. Returns confirmation ID and ETA in minutes.")
    public Map<String, Object> dispatchFuelDelivery(double latitude, double longitude) {
        String confirmationId = "FUEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int etaMinutes = ThreadLocalRandom.current().nextInt(25, 45);

        return Map.of(
                "confirmationId", confirmationId,
                "etaMinutes", etaMinutes,
                "provider", "Allstate Roadside Fuel Delivery",
                "status", "DISPATCHED",
                "message", "Fuel delivery has been dispatched. Confirmation: " + confirmationId
        );
    }

    @Tool(description = "Dispatch a battery jump-start technician to the given coordinates. Returns confirmation ID and ETA in minutes.")
    public Map<String, Object> dispatchJumpStart(double latitude, double longitude) {
        String confirmationId = "BATT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int etaMinutes = ThreadLocalRandom.current().nextInt(20, 40);

        return Map.of(
                "confirmationId", confirmationId,
                "etaMinutes", etaMinutes,
                "provider", "Allstate Battery Assist",
                "status", "DISPATCHED",
                "message", "Jump-start technician has been dispatched. Confirmation: " + confirmationId
        );
    }

    @Tool(description = "Dispatch a flat tyre repair technician to the given coordinates. Returns confirmation ID and ETA in minutes.")
    public Map<String, Object> dispatchTyreRepair(double latitude, double longitude) {
        String confirmationId = "TYRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int etaMinutes = ThreadLocalRandom.current().nextInt(30, 50);

        return Map.of(
                "confirmationId", confirmationId,
                "etaMinutes", etaMinutes,
                "provider", "Allstate Tyre Care",
                "status", "DISPATCHED",
                "message", "Tyre repair technician has been dispatched. Confirmation: " + confirmationId
        );
    }
}
