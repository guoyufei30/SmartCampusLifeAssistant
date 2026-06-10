package com.smartcampuslifeserver.dto.analysis;

import lombok.Data;

@Data
public class PressureAnalysisResponse {

    private Integer apiScore;
    private String level;
    private String levelText;
    private Breakdown breakdown;

    @Data
    public static class Breakdown {
        private Integer courseHours;
        private Double courseContribution;
        private Integer examCount;
        private Double examContribution;
        private Integer ddlCount;
        private Double ddlContribution;
        private Double completionRate;
        private Double completionContribution;
    }
}
