package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.*;

public interface AuthService {

    Result<RegisterResponse> register(RegisterRequest request);

    Result<LoginResponse> login(LoginRequest request);

    Result<Void> logout(String authorizationHeader);

    Result<RefreshTokenResponse> refresh(RefreshTokenRequest request);

    Result<SendVerifyCodeResponse> sendVerifyCode(SendVerifyCodeRequest request);

    boolean isTokenBlacklisted(String token);
}
