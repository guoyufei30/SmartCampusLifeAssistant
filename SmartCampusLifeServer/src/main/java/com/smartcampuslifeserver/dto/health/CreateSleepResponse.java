package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class CreateSleepResponse {

    private Boolean requiresConfirm;
    private Double duration;
    private SleepRecordResponse record;
}
