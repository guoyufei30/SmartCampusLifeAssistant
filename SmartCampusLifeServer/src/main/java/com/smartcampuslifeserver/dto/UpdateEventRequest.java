package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class UpdateEventRequest {

    private String title;
    private String category;
    private String startTime;
    private String endTime;
    private String deadline;
    private String location;
    private String remark;
    private String reminder;
}
