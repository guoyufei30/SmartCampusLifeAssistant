package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class CreateWeightRequest {

    private Double weight;
    private String recordDate;
}
