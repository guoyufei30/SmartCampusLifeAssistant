package com.smartcampuslifeserver.dto.analysis;

import lombok.Data;

@Data
public class ProcrastinationAnalysisResponse {

    private Integer weekCompletionRate;
    private Integer onTimeRate;
    private Integer threshold;
    private String alert;
    private String suggestion;
}
