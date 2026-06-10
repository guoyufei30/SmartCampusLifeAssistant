package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.*;
import com.smartcampuslifeserver.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        return userService.getProfile(SecurityUtils.getUserId());
    }

    @PutMapping("/profile")
    public Result<UserProfileResponse> updateProfile(@RequestBody UpdateUserRequest request) {
        return userService.updateProfile(SecurityUtils.getUserId(), request);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        return userService.changePassword(SecurityUtils.getUserId(), request);
    }

    @PostMapping("/force_password")
    public Result<Void> forceChangePassword(@RequestBody ForceChangePasswordRequest request) {
        return userService.forceChangePassword(SecurityUtils.getUserId(), request);
    }

    @PostMapping("/avatar")
    public Result<AvatarUploadResponse> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        return userService.uploadAvatar(SecurityUtils.getUserId(), avatar);
    }

    @PutMapping("/phone/bind")
    public Result<ForceLogoutResponse> bindPhone(@RequestBody BindPhoneRequest request) {
        return userService.bindPhone(SecurityUtils.getUserId(), request);
    }

    @DeleteMapping("/account")
    public Result<ForceLogoutResponse> deleteAccount(@RequestBody DeleteAccountRequest request) {
        return userService.deleteAccount(SecurityUtils.getUserId(), request);
    }
}
