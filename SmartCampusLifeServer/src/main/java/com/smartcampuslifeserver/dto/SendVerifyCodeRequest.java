package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class SendVerifyCodeRequest {

    private String phone;
    private String type;
}
