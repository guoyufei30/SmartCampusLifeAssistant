package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class CreateCourseRequest {

    private String title;
    private String weekPattern;
    private Integer dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String location;
}
