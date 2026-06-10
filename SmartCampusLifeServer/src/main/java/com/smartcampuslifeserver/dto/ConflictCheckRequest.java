package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class ConflictCheckRequest {

    private String category;
    private String startTime;
    private String endTime;
    private String excludeId;
}
