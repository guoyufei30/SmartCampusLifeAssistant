package com.smartcampuslifeserver.dto.report;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WeeklyReportResponse {

    private Long id;
    private Integer weekNumber;
    private String startDate;
    private String endDate;
    private Boolean canRegenerate;
    private String regenerateAvailableUntil;
    private Map<String, Object> sections;
    private List<String> suggestions;
}
