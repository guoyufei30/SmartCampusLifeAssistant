package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.JwtUtil;
import com.smartcampuslifeserver.dto.*;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.entity.VerifyCode;
import com.smartcampuslifeserver.repository.UserRepository;
import com.smartcampuslifeserver.repository.VerifyCodeRepository;
import com.smartcampuslifeserver.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int VERIFY_CODE_EXPIRE_SECONDS = 300;
    private static final Set<String> VERIFY_CODE_TYPES = Set.of("register", "bind", "admin_create");

    private final UserRepository userRepository;
    private final VerifyCodeRepository verifyCodeRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepository userRepository,
                           VerifyCodeRepository verifyCodeRepository,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verifyCodeRepository = verifyCodeRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Result<RegisterResponse> register(RegisterRequest request) {
        validatePhone(request.getPhone());
        validatePassword(request.getPassword());
        validateVerifyCodeFormat(request.getVerifyCode());

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BusinessException(409, "该手机号已注册");
        }

        verifyCode(request.getPhone(), "register", request.getVerifyCode());

        User user = new User();
        user.setUserId("usr_" + UUID.randomUUID().toString().substring(0, 8));
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname("用户_" + request.getPhone().substring(7));
        user.setRole("user");
        user.setStatus("normal");
        user.setForceChangePassword(false);
        userRepository.save(user);

        RegisterResponse response = new RegisterResponse();
        response.setUserId(user.getUserId());
        response.setToken(jwtUtil.generateToken(user.getUserId(), user.getRole()));

        return Result.success(response, "注册成功");
    }

    @Override
    @Transactional
    public Result<LoginResponse> login(LoginRequest request) {
        if (request.getPhone() == null || request.getPhone().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(400, "手机号和密码不能为空");
        }

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(401, "手机号或密码错误"));

        if (!matchesPassword(request.getPassword(), user)) {
            throw new BusinessException(401, "手机号或密码错误");
        }

        if ("frozen".equals(user.getStatus())) {
            String reasonText = getFreezeReasonText(user.getFrozenReason());
            throw new BusinessException(403,
                    "您的账号因[" + reasonText + "]已被冻结，请联系管理员",
                    "account_frozen");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        LoginResponse response = buildLoginResponse(user);
        if (Boolean.TRUE.equals(user.getForceChangePassword())) {
            return Result.success(response, "登录成功，请先修改临时密码");
        }
        return Result.success(response, "登录成功");
    }

    @Override
    public Result<Void> logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token != null && jwtUtil.validateToken(token) && !jwtUtil.isRefreshToken(token)) {
            blacklistToken(token);
        }
        return Result.success(null, "登出成功");
    }

    @Override
    public Result<RefreshTokenResponse> refresh(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BusinessException(400, "refreshToken 不能为空");
        }

        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "refreshToken 无效或已过期");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "无效的 refreshToken");
        }
        if (isTokenBlacklisted(refreshToken)) {
            throw new BusinessException(401, "refreshToken 已失效");
        }

        String userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        if ("frozen".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被冻结，请联系管理员", "account_frozen");
        }

        blacklistToken(refreshToken);

        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setToken(jwtUtil.generateToken(user.getUserId(), user.getRole()));
        response.setRefreshToken(jwtUtil.generateRefreshToken(user.getUserId()));
        response.setExpiresIn(jwtUtil.getAccessTokenExpiresInSeconds());

        return Result.success(response, "刷新成功");
    }

    @Override
    @Transactional
    public Result<SendVerifyCodeResponse> sendVerifyCode(SendVerifyCodeRequest request) {
        validatePhone(request.getPhone());
        if (request.getType() == null || !VERIFY_CODE_TYPES.contains(request.getType())) {
            throw new BusinessException(400, "验证码类型无效");
        }

        String code = String.format("%06d", (int) (Math.random() * 1_000_000));

        VerifyCode verifyCode = new VerifyCode();
        verifyCode.setPhone(request.getPhone());
        verifyCode.setType(request.getType());
        verifyCode.setCode(code);
        verifyCode.setExpireTime(LocalDateTime.now().plusSeconds(VERIFY_CODE_EXPIRE_SECONDS));
        verifyCodeRepository.save(verifyCode);

        System.out.printf("[验证码模拟] 手机号: %s, 类型: %s, 验证码: %s, 有效期: %d秒%n",
                request.getPhone(), request.getType(), code, VERIFY_CODE_EXPIRE_SECONDS);

        SendVerifyCodeResponse response = new SendVerifyCodeResponse();
        response.setExpireSeconds(VERIFY_CODE_EXPIRE_SECONDS);

        return Result.success(response, "验证码已发送，有效时间为5分钟");
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        cleanExpiredTokens();
        Long expireAt = tokenBlacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            tokenBlacklist.remove(token);
            return false;
        }
        return true;
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getUserId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar() != null
                ? user.getAvatar()
                : "https://cdn.example.com/avatar/default.png");
        response.setToken(jwtUtil.generateToken(user.getUserId(), user.getRole()));
        response.setRefreshToken(jwtUtil.generateRefreshToken(user.getUserId()));
        response.setExpiresIn(jwtUtil.getAccessTokenExpiresInSeconds());
        response.setRole(user.getRole());
        if (Boolean.TRUE.equals(user.getForceChangePassword())) {
            response.setForceChangePassword(true);
        }
        return response;
    }

    private boolean matchesPassword(String rawPassword, User user) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }

        if (rawPassword.equals(storedPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    private void verifyCode(String phone, String type, String code) {
        VerifyCode verifyCode = verifyCodeRepository.findByPhoneAndType(phone, type)
                .orElseThrow(() -> new BusinessException(400, "验证码错误或已过期"));

        if (verifyCode.getExpireTime().isBefore(LocalDateTime.now())) {
            verifyCodeRepository.delete(verifyCode);
            throw new BusinessException(400, "验证码错误或已过期");
        }
        if (!verifyCode.getCode().equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        verifyCodeRepository.delete(verifyCode);
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }
    }

    private void validatePassword(String password) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$")) {
            throw new BusinessException(400, "密码须为8-16位，且包含字母与数字");
        }
    }

    private void validateVerifyCodeFormat(String code) {
        if (code == null || !code.matches("^\\d{6}$")) {
            throw new BusinessException(400, "验证码格式不正确");
        }
    }

    private void blacklistToken(String token) {
        try {
            tokenBlacklist.put(token, jwtUtil.getExpirationMs(token));
            cleanExpiredTokens();
        } catch (Exception ignored) {
            // 无效 token 无需加入黑名单
        }
    }

    private void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private String getFreezeReasonText(String reasonCode) {
        if (reasonCode == null) {
            return "未知原因";
        }
        return switch (reasonCode) {
            case "content_violation" -> "内容违规";
            case "security_risk" -> "安全风险";
            case "abnormal_checkin" -> "异常打卡";
            case "other" -> "其他";
            default -> reasonCode;
        };
    }
}
