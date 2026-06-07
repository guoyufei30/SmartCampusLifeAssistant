package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class AnnouncementResponse {

    private Long id;
    private String content;
    private String type;
    private Boolean dismissible;
}
