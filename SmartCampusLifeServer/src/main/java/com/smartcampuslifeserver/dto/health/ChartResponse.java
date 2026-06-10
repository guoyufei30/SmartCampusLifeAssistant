package com.smartcampuslifeserver.dto.health;

import lombok.Data;

import java.util.List;

@Data
public class ChartResponse {

    private String type;
    private String startDate;
    private String endDate;
    private List<String> xAxis;
    private List<Double> yAxis;
    private String unit;
}
