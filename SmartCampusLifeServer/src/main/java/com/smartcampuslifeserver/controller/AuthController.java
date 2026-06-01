package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 用户注册 ====================

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        // 检查手机号是否已注册
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return result(409, "该手机号已注册", null);
        }

        // 验证短信验证码（简化：暂时不做真实验证）
        if (!verifyCode(request.getPhone(), request.getVerifyCode())) {
            return result(400, "验证码错误或已过期", null);
        }

        // 创建用户
        User user = new User();
        user.setUserId("usr_" + UUID.randomUUID().toString().substring(0, 8));
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword());
        user.setNickname("用户_" + request.getPhone().substring(7));
        user.setRole("user");
        user.setStatus("normal");
        user.setCreateTime(LocalDateTime.now());
        userRepository.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("token", "token_" + UUID.randomUUID().toString());
        data.put("refreshToken", "refresh_" + UUID.randomUUID().toString());

        return result(200, "注册成功", data);
    }

    // ==================== 用户登录 ====================

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        Optional<User> optUser = userRepository.findByPhone(request.getPhone());

        if (optUser.isEmpty()) {
            return result(401, "手机号或密码错误", null);
        }

        User user = optUser.get();

        // 检查密码
        if (!request.getPassword().equals(user.getPassword())) {
            return result(401, "手机号或密码错误", null);
        }

        // 检查账号状态
        if ("frozen".equals(user.getStatus())) {
            Map<String, Object> data = new HashMap<>();
            data.put("subCode", "account_frozen");
            return result(403, "您的账号已被冻结，请联系管理员", data);
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        // 生成 Token（简化版，实际应使用 JWT）
        String token = "token_" + UUID.randomUUID().toString();
        String refreshToken = "refresh_" + UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "https://cdn.example.com/avatar/default.png");
        data.put("token", token);
        data.put("refreshToken", refreshToken);
        data.put("expiresIn", 604800);
        data.put("role", user.getRole());

        return result(200, "登录成功", data);
    }

    // ==================== 用户登出 ====================

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 简化处理：实际应验证 Token 并从黑名单/缓存中移除
        return result(200, "登出成功", null);
    }

    // ==================== 发送短信验证码 ====================

    @PostMapping("/send_verify_code")
    public Map<String, Object> sendVerifyCode(@RequestBody SendVerifyCodeRequest request) {
        // 简化实现：模拟发送验证码
        // 实际项目中应调用短信服务商 API（如阿里云、腾讯云）

        // 模拟生成 6 位验证码（实际应该发送到手机）
        String code = String.format("%06d", new Random().nextInt(999999));

        // 存储验证码（实际应存入 Redis，设置过期时间）
        // 这里简化处理，实际项目需要配合 Redis 或数据库

        Map<String, Object> data = new HashMap<>();
        data.put("expireSeconds", 300);

        return result(200, "验证码已发送，有效时间为5分钟", data);
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> result(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }

    /**
     * 验证验证码（简化实现）
     * 实际项目中应从 Redis 或数据库读取存储的验证码进行比对
     */
    private boolean verifyCode(String phone, String code) {
        // 暂时跳过验证码验证，方便测试
        // 实际项目需要实现真实的验证码存储和验证逻辑
        return code != null && code.length() == 6;
    }

    // ==================== 请求 DTO ====================

    @Data
    public static class RegisterRequest {
        private String phone;
        private String password;
        private String verifyCode;
    }

    @Data
    public static class LoginRequest {
        private String phone;
        private String password;
    }

    @Data
    public static class SendVerifyCodeRequest {
        private String phone;
        private String type;
    }
}
