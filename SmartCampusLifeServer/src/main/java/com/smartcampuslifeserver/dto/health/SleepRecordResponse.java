package com.smartcampuslifeserver.dto.health;

import lombok.Data;

@Data
public class SleepRecordResponse {

    private String id;
    private String sleepDate;
    private String bedTime;
    private String wakeTime;
    private Double duration;
    private String quality;
    private String createTime;
}
