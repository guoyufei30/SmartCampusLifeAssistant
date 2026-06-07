package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class ScheduleEventResponse {

    private String id;
    private String title;
    private String category;
    private String weekPattern;
    private Integer dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String startTime;
    private String endTime;
    private String deadline;
    private String location;
    private String remark;
    private String reminder;
    private String status;
    private String countdown;
}
