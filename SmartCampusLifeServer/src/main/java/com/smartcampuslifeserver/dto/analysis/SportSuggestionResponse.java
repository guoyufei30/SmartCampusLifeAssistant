package com.smartcampuslifeserver.dto.analysis;

import lombok.Data;

import java.util.List;

@Data
public class SportSuggestionResponse {

    private Boolean hasSuggestion;
    private List<SuggestionItem> suggestions;

    @Data
    public static class SuggestionItem {
        private String startTime;
        private String endTime;
        private Integer duration;
        private String message;
    }
}
