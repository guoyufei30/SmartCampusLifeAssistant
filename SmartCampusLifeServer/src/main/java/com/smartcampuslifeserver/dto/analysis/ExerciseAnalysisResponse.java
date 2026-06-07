package com.smartcampuslifeserver.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class ExerciseAnalysisResponse {

    private Integer defaultTarget;
    private Integer adjustedTarget;
    private String adjustReason;
    private Integer actualMinutes;
    private Double progress;
    private String status;
    private List<String> suggestions;
}
