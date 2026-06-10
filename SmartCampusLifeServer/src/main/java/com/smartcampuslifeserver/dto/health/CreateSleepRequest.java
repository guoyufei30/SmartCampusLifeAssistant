package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class CreateSleepRequest {

    private String bedTime;
    private String wakeTime;
    private String quality;
    private String recordDate;
    private Boolean confirmAbnormal;
}
