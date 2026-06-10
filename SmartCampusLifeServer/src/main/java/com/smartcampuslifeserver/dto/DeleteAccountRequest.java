package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class DeleteAccountRequest {

    private String password;
    private Boolean confirmRisk;
}
