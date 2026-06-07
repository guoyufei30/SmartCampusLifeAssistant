package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class SemesterResponse {

    private Long id;
    private String name;
    private String startDate;
    private String endDate;
}
