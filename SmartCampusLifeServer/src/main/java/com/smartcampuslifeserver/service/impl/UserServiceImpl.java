package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.*;
import com.smartcampuslifeserver.entity.ExceptionLog;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.entity.VerifyCode;
import com.smartcampuslifeserver.entity.WeightRecord;
import com.smartcampuslifeserver.repository.*;
import com.smartcampuslifeserver.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;

    private final Path avatarDir;

    private static final Set<String> GENDERS = Set.of("未知", "男", "女");
    private static final Set<String> GRADES = Set.of("大一", "大二", "大三", "大四", "研究生");
    private static final List<String> SENSITIVE_WORDS = List.of(
            "违禁", "管理员", "暴力", "色情", "赌博", "fuck", "admin", "傻逼", "毒品"
    );

    private final UserRepository userRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final VerifyCodeRepository verifyCodeRepository;
    private final ExceptionLogRepository exceptionLogRepository;
    private final CourseRepository courseRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserAnnouncementRepository userAnnouncementRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           WeightRecordRepository weightRecordRepository,
                           VerifyCodeRepository verifyCodeRepository,
                           ExceptionLogRepository exceptionLogRepository,
                           CourseRepository courseRepository,
                           ScheduleEventRepository scheduleEventRepository,
                           SleepRecordRepository sleepRecordRepository,
                           ExerciseRecordRepository exerciseRecordRepository,
                           WeeklyReportRepository weeklyReportRepository,
                           UserAnnouncementRepository userAnnouncementRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.userRepository = userRepository;
        this.weightRecordRepository = weightRecordRepository;
        this.verifyCodeRepository = verifyCodeRepository;
        this.exceptionLogRepository = exceptionLogRepository;
        this.courseRepository = courseRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.sleepRecordRepository = sleepRecordRepository;
        this.exerciseRecordRepository = exerciseRecordRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.userAnnouncementRepository = userAnnouncementRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
    }

    @Override
    public Result<UserProfileResponse> getProfile(String userId) {
        User user = getUserAndCheckTempPassword(userId);
        return Result.success(toProfileResponse(user), "获取成功");
    }

    @Override
    @Transactional
    public Result<UserProfileResponse> updateProfile(String userId, UpdateUserRequest request) {
        User user = getUserAndCheckTempPassword(userId);

        if (request.getNickname() != null) {
            validateNickname(request.getNickname());
            checkSensitiveWords(userId, request.getNickname(), "/user/profile");
            user.setNickname(request.getNickname());
        }
        if (request.getGender() != null) {
            if (!GENDERS.contains(request.getGender())) {
                throw new BusinessException(400, "性别参数无效");
            }
            user.setGender(request.getGender());
        }
        if (request.getGrade() != null) {
            if (!GRADES.contains(request.getGrade())) {
                throw new BusinessException(400, "年级参数无效");
            }
            user.setGrade(request.getGrade());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getHeight() != null) {
            if (request.getHeight() < 100 || request.getHeight() > 250) {
                throw new BusinessException(400, "身高须在100-250cm之间");
            }
            user.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            if (request.getWeight() < 30 || request.getWeight() > 250) {
                throw new BusinessException(400, "体重须在30-250kg之间");
            }
            user.setWeight(request.getWeight());
            syncWeightRecord(userId, request.getWeight());
        }

        userRepository.save(user);
        return Result.success(toProfileResponse(user), "更新成功");
    }

    @Override
    @Transactional
    public Result<Void> changePassword(String userId, ChangePasswordRequest request) {
        User user = getUserAndCheckTempPassword(userId);
        validatePasswordRequest(request.getOldPassword(), request.getNewPassword());

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return Result.success(null, "密码修改成功");
    }

    @Override
    @Transactional
    public Result<Void> forceChangePassword(String userId, ForceChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (!Boolean.TRUE.equals(user.getForceChangePassword())) {
            throw new BusinessException(400, "当前账号无需强制修改密码");
        }
        if (request.getNewPassword() == null) {
            throw new BusinessException(400, "新密码不能为空");
        }
        validatePasswordFormat(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForceChangePassword(false);
        userRepository.save(user);
        return Result.success(null, "密码修改成功");
    }

    @Override
    @Transactional
    public Result<AvatarUploadResponse> uploadAvatar(String userId, MultipartFile avatar) {
        User user = getUserAndCheckTempPassword(userId);

        if (avatar == null || avatar.isEmpty()) {
            throw new BusinessException(400, "请上传头像文件");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(400, "头像文件大小不能超过5MB");
        }

        String contentType = avatar.getContentType();
        String originalFilename = avatar.getOriginalFilename();
        String extension = resolveImageExtension(contentType, originalFilename);
        if (extension == null) {
            throw new BusinessException(400, "仅支持JPG/PNG格式图片");
        }

        try {
            Files.createDirectories(avatarDir);
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path target = avatarDir.resolve(filename);
            try (InputStream inputStream = avatar.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String avatarUrl = "/uploads/avatars/" + filename;
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            AvatarUploadResponse response = new AvatarUploadResponse();
            response.setAvatarUrl(avatarUrl);
            return Result.success(response, "上传成功");
        } catch (IOException e) {
            throw new BusinessException(500, "头像上传失败");
        }
    }

    @Override
    @Transactional
    public Result<ForceLogoutResponse> bindPhone(String userId, BindPhoneRequest request) {
        User user = getUserAndCheckTempPassword(userId);

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "登录密码错误");
        }
        validatePhone(request.getNewPhone());
        validateVerifyCodeFormat(request.getVerifyCode());
        verifyCode(request.getNewPhone(), "bind", request.getVerifyCode());

        if (userRepository.findByPhone(request.getNewPhone()).isPresent()) {
            throw new BusinessException(409, "该手机号已被其他账号绑定，请更换");
        }

        user.setPhone(request.getNewPhone());
        userRepository.save(user);

        ForceLogoutResponse response = new ForceLogoutResponse();
        response.setForceLogout(true);
        return Result.success(response, "换绑成功，请使用新手机号重新登录");
    }

    @Override
    @Transactional
    public Result<ForceLogoutResponse> deleteAccount(String userId, DeleteAccountRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (!Boolean.TRUE.equals(request.getConfirmRisk())) {
            throw new BusinessException(400, "请确认了解注销风险");
        }
        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        deleteUserRelatedData(userId);
        userRepository.delete(user);

        ForceLogoutResponse response = new ForceLogoutResponse();
        response.setForceLogout(true);
        return Result.success(response, "账号及个人数据已成功注销并清除");
    }

    private void deleteUserRelatedData(String userId) {
        courseRepository.deleteByUserId(userId);
        scheduleEventRepository.deleteByUserId(userId);
        sleepRecordRepository.deleteByUserId(userId);
        exerciseRecordRepository.deleteByUserId(userId);
        weightRecordRepository.deleteByUserId(userId);
        weeklyReportRepository.deleteByUserId(userId);
        exceptionLogRepository.deleteByUserId(userId);
        userAnnouncementRepository.deleteByUserId(userId);
    }

    private void syncWeightRecord(String userId, Double weight) {
        LocalDate today = LocalDate.now();
        BigDecimal weightValue = BigDecimal.valueOf(weight).setScale(1, RoundingMode.HALF_UP);

        WeightRecord record = weightRecordRepository.findByUserIdAndRecordDate(userId, today)
                .orElseGet(() -> {
                    WeightRecord newRecord = new WeightRecord();
                    newRecord.setUserId(userId);
                    newRecord.setRecordDate(today);
                    return newRecord;
                });
        record.setWeight(weightValue);
        weightRecordRepository.save(record);
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getUserId());
        response.setPhone(maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar() != null
                ? user.getAvatar()
                : "https://cdn.example.com/avatar/default.png");
        response.setGender(user.getGender() != null ? user.getGender() : "未知");
        response.setGrade(user.getGrade() != null ? user.getGrade() : "大一");
        response.setBirthDate(user.getBirthDate() != null ? user.getBirthDate() : "未设置");
        response.setDepartment(user.getDepartment() != null ? user.getDepartment() : "未设置");
        response.setHeight(user.getHeight() != null ? String.valueOf(user.getHeight()) : "未设置");
        response.setWeight(user.getWeight() != null ? String.valueOf(user.getWeight()) : "未设置");
        response.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().format(FORMATTER) : null);
        return response;
    }

    private User getUserAndCheckTempPassword(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (Boolean.TRUE.equals(user.getForceChangePassword())) {
            throw new BusinessException(403, "请先修改临时密码", "temp_password_required");
        }
        return user;
    }

    private void validateNickname(String nickname) {
        if (!nickname.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]{2,15}$")) {
            throw new BusinessException(400, "昵称须为2-15位中英文、数字或下划线");
        }
    }

    private void checkSensitiveWords(String userId, String content, String requestUrl) {
        String lower = content.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                ExceptionLog log = new ExceptionLog();
                log.setUserId(userId);
                log.setExceptionType("content_violation");
                log.setExceptionDetail("昵称包含违禁词：" + word);
                log.setRequestUrl(requestUrl);
                exceptionLogRepository.save(log);
                throw new BusinessException(400, "内容包含违禁词汇，请修改后重试");
            }
        }
    }

    private void validatePasswordRequest(String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException(400, "原密码不能为空");
        }
        validatePasswordFormat(newPassword);
    }

    private void validatePasswordFormat(String password) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$")) {
            throw new BusinessException(400, "密码须为8-16位，且包含字母与数字");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }
    }

    private void validateVerifyCodeFormat(String code) {
        if (code == null || !code.matches("^\\d{6}$")) {
            throw new BusinessException(400, "验证码格式不正确");
        }
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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String resolveImageExtension(String contentType, String originalFilename) {
        if (contentType != null) {
            String type = contentType.toLowerCase();
            if (type.equals("image/jpeg") || type.equals("image/jpg") || type.equals("image/pjpeg")) {
                return ".jpg";
            }
            if (type.equals("image/png") || type.equals("image/x-png")) {
                return ".png";
            }
        }
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return ".jpg";
            }
            if (lower.endsWith(".png")) {
                return ".png";
            }
        }
        return null;
    }
}
