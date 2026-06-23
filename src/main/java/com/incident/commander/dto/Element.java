package com.incident.commander.dto;

import lombok.Data;

import java.util.Map;

@Data
public class Element {
    private double lat;
    private double lon;
    private Map<String, String> tags;
}
