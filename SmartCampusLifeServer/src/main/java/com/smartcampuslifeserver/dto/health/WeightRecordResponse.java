package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class WeightRecordResponse {

    private String id;
    private Double weight;
    private String recordDate;
    private String createTime;
}
