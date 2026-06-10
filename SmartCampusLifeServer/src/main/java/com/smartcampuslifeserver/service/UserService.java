package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    Result<UserProfileResponse> getProfile(String userId);

    Result<UserProfileResponse> updateProfile(String userId, UpdateUserRequest request);

    Result<Void> changePassword(String userId, ChangePasswordRequest request);

    Result<Void> forceChangePassword(String userId, ForceChangePasswordRequest request);

    Result<AvatarUploadResponse> uploadAvatar(String userId, MultipartFile avatar);

    Result<ForceLogoutResponse> bindPhone(String userId, BindPhoneRequest request);

    Result<ForceLogoutResponse> deleteAccount(String userId, DeleteAccountRequest request);
}
