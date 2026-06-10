package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class CreateExerciseRequest {

    private String type;
    private Integer duration;
    private Integer count;
    private String recordDate;
}
