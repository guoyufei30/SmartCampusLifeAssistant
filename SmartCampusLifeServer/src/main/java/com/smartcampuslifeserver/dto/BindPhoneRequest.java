package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class BindPhoneRequest {

    private String password;
    private String newPhone;
    private String verifyCode;
}
