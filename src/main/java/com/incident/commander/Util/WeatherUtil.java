package com.incident.commander.Util;

public class WeatherUtil {

    public static String getCondition(int code) {

        return switch (code) {
            case 0 -> "Clear Sky";
            case 1, 2, 3 -> "Cloudy";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 80, 81, 82 -> "Rain Showers";
            case 95 -> "Thunderstorm";
            default -> "Unknown";
        };
    }
}
