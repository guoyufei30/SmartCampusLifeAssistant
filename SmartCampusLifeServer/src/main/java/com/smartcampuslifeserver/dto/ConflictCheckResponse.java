package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class ConflictCheckResponse {

    private Boolean hasConflict;
    private ConflictEventInfo conflictEvent;

    @Data
    public static class ConflictEventInfo {
        private String id;
        private String title;
        private String category;
    }
}
