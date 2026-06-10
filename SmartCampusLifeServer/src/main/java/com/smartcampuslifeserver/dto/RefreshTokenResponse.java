package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class RefreshTokenResponse {

    private String token;
    private String refreshToken;
    private Long expiresIn;
}
