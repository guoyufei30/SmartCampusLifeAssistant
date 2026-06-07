package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class ReminderResponse {

    private String id;
    private String title;
    private String reminder;
    private String reminderTime;
}
