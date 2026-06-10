package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class ExerciseRecordResponse {

    private String id;
    private String type;
    private Integer duration;
    private Integer count;
    private String recordDate;
    private String createTime;
}
