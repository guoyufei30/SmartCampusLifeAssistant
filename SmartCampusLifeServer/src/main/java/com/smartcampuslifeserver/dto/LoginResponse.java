package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String userId;
    private String nickname;
    private String avatar;
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private String role;
    private Boolean forceChangePassword;
}
