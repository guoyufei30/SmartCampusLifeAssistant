package com.smartcampuslifeserver.dto.report;

import lombok.Data;

@Data
public class WeeklyReportHistoryItem {

    private Long id;
    private Integer weekNumber;
    private String startDate;
    private String endDate;
}
