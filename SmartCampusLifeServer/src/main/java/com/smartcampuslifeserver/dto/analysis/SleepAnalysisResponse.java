package com.smartcampuslifeserver.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class SleepAnalysisResponse {

    private List<AlertItem> alerts;
    private Summary summary;

    @Data
    public static class AlertItem {
        private String type;
        private String suggestion;
    }

    @Data
    public static class Summary {
        private Double avgDuration;
        private String avgQuality;
    }
}
