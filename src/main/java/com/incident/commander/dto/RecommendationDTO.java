package com.incident.commander.dto;

import lombok.Data;

@Data
public class RecommendationDTO {
    private Integer rank;
    private String action;
    private String reason;
    private String eta;
    private String cost;
}