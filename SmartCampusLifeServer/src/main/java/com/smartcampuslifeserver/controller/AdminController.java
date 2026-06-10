package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.entity.*;
import com.smartcampuslifeserver.repository.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private ExceptionLogRepository exceptionLogRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 仪表盘 ====================

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalAdmins = userRepository.countByRole("admin") + userRepository.countByRole("super_admin");
        long totalSuperAdmins = userRepository.countByRole("super_admin");
        long frozenUsers = userRepository.countByStatus("frozen");

        data.put("totalUsers", totalUsers);
        data.put("totalAdmins", totalAdmins);
        data.put("totalSuperAdmins", totalSuperAdmins);
        data.put("frozenUsers", frozenUsers);
        data.put("activeUsers", totalUsers - frozenUsers);

        return result(200, "获取成功", data);
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Map<String, Object> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<User> userPage;

        if (keyword != null && !keyword.isEmpty()) {
            if (status != null && !status.isEmpty()) {
                userPage = userRepository.findByNicknameContainingOrPhoneContainingOrUserIdContainingAndStatus(
                    keyword, keyword, keyword, status, pageRequest);
            } else {
                userPage = userRepository.findByNicknameContainingOrPhoneContainingOrUserIdContaining(
                    keyword, keyword, keyword, pageRequest);
            }
        } else if (status != null && !status.isEmpty()) {
            userPage = userRepository.findByStatus(status, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (User user : userPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", user.getUserId());
            item.put("phone", maskPhone(user.getPhone()));
            item.put("nickname", user.getNickname());
            item.put("gender", user.getGender());
            item.put("grade", user.getGrade());
            item.put("status", user.getStatus());
            item.put("freezeReasonCode", "frozen".equals(user.getStatus()) ? user.getFrozenReason() : null);
            item.put("freezeReasonText", "frozen".equals(user.getStatus()) ? getReasonText(user.getFrozenReason()) : null);
            item.put("freezeTime", user.getFrozenTime());
            item.put("createTime", user.getCreateTime() != null ? user.getCreateTime().format(FORMATTER) : null);
            item.put("lastLoginTime", user.getLastLoginTime() != null ? user.getLastLoginTime().format(FORMATTER) : null);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", userPage.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);

        return result(200, "获取成功", data);
    }

    @PostMapping("/users/{userId}/freeze")
    public Map<String, Object> freezeUser(@PathVariable String userId, @RequestBody Map<String, String> body) {
        Optional<User> optUser = userRepository.findByUserId(userId);
        if (optUser.isEmpty()) {
            return result(404, "用户不存在", null);
        }

        User user = optUser.get();
        String reasonCode = body.get("reasonCode");
        user.setStatus("frozen");
        user.setFrozenTime(LocalDateTime.now().format(FORMATTER));
        user.setFrozenReason(reasonCode);
        userRepository.save(user);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("freeze_user");
        log.setActionText("冻结用户");
        log.setTargetType("user");
        log.setTargetId(userId);
        log.setTargetNickname(user.getNickname());
        log.setReasonCode(reasonCode);
        log.setReasonText(getReasonText(reasonCode));
        operationLogRepository.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("freezeTime", user.getFrozenTime());
        data.put("reasonCode", reasonCode);
        data.put("reasonText", getReasonText(reasonCode));

        return result(200, "冻结成功", data);
    }

    @PostMapping("/users/{userId}/unfreeze")
    public Map<String, Object> unfreezeUser(@PathVariable String userId) {
        Optional<User> optUser = userRepository.findByUserId(userId);
        if (optUser.isEmpty()) {
            return result(404, "用户不存在", null);
        }

        User user = optUser.get();
        user.setStatus("normal");
        user.setFrozenTime(null);
        user.setFrozenReason(null);
        userRepository.save(user);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("unfreeze_user");
        log.setActionText("解封用户");
        log.setTargetType("user");
        log.setTargetId(userId);
        log.setTargetNickname(user.getNickname());
        operationLogRepository.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("unfreezeTime", LocalDateTime.now().format(FORMATTER));

        return result(200, "解封成功", data);
    }

    @PostMapping("/users/{userId}/reset_password")
    public Map<String, Object> resetPassword(@PathVariable String userId, @RequestBody Map<String, String> body) {
        Optional<User> optUser = userRepository.findByUserId(userId);
        if (optUser.isEmpty()) {
            return result(404, "用户不存在", null);
        }

        String tempPassword = body.get("tempPassword");
        User user = optUser.get();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setForceChangePassword(true);
        userRepository.save(user);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("reset_password");
        log.setActionText("重置密码");
        log.setTargetType("user");
        log.setTargetId(userId);
        log.setTargetNickname(user.getNickname());
        operationLogRepository.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("tempPassword", tempPassword);
        data.put("forceChange", true);

        return result(200, "密码重置成功", data);
    }

    // ==================== 公告管理 ====================

    @GetMapping("/announcements")
    public Map<String, Object> getAnnouncements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Announcement> pageResult;

        if (status != null && !status.isEmpty() && type != null && !type.isEmpty()) {
            pageResult = announcementRepository.findByStatusAndType(status, type, pageRequest);
        } else if (status != null && !status.isEmpty()) {
            pageResult = announcementRepository.findByStatus(status, pageRequest);
        } else if (type != null && !type.isEmpty()) {
            pageResult = announcementRepository.findByType(type, pageRequest);
        } else {
            pageResult = announcementRepository.findAll(pageRequest);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Announcement a : pageResult.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("content", a.getContent());
            item.put("type", a.getType());
            item.put("dismissible", a.getDismissible());
            item.put("status", a.getStatus());
            item.put("createTime", a.getCreateTime() != null ? a.getCreateTime().format(FORMATTER) : null);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);

        return result(200, "获取成功", data);
    }

    @PostMapping("/announcements")
    public Map<String, Object> publishAnnouncement(@RequestBody Map<String, String> body) {
        Announcement announcement = new Announcement();
        announcement.setContent(body.get("content"));
        announcement.setType(body.get("type"));
        announcement.setStatus("active");
        announcement.setDismissible(true);
        announcementRepository.save(announcement);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("publish_announcement");
        log.setActionText("发布公告");
        log.setTargetType("announcement");
        log.setTargetId(String.valueOf(announcement.getId()));
        operationLogRepository.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("id", announcement.getId());

        return result(200, "发布成功", data);
    }

    @PutMapping("/announcements/{id}/offline")
    public Map<String, Object> offlineAnnouncement(@PathVariable Long id) {
        Optional<Announcement> optAnn = announcementRepository.findById(id);
        if (optAnn.isEmpty()) {
            return result(404, "公告不存在", null);
        }

        Announcement announcement = optAnn.get();
        announcement.setStatus("offline");
        announcementRepository.save(announcement);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("offline_announcement");
        log.setActionText("下架公告");
        log.setTargetType("announcement");
        log.setTargetId(String.valueOf(id));
        operationLogRepository.save(log);

        return result(200, "下架成功", null);
    }

    // ==================== 日志管理 ====================

    @GetMapping("/logs/operation")
    public Map<String, Object> getOperationLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OperationLog> pageResult;

        if (action != null && !action.isEmpty() && targetType != null && !targetType.isEmpty()) {
            pageResult = operationLogRepository.findByActionAndTargetType(action, targetType, pageRequest);
        } else if (action != null && !action.isEmpty()) {
            pageResult = operationLogRepository.findByAction(action, pageRequest);
        } else if (targetType != null && !targetType.isEmpty()) {
            pageResult = operationLogRepository.findByTargetType(targetType, pageRequest);
        } else {
            pageResult = operationLogRepository.findAll(pageRequest);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (OperationLog log : pageResult.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("logId", log.getId());
            item.put("adminId", log.getAdminId());
            item.put("adminNickname", log.getAdminNickname());
            item.put("action", log.getAction());
            item.put("actionText", log.getActionText());
            item.put("targetType", log.getTargetType());
            item.put("targetId", log.getTargetId());
            item.put("targetNickname", log.getTargetNickname());
            item.put("reasonCode", log.getReasonCode());
            item.put("reasonText", log.getReasonText());
            item.put("details", log.getDetails());
            item.put("ipAddress", log.getIpAddress());
            item.put("createTime", log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : null);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);

        return result(200, "获取成功", data);
    }

    @GetMapping("/logs/exception")
    public Map<String, Object> getExceptionLogs(
            @RequestParam(required = false) String exceptionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ExceptionLog> pageResult;

        if (exceptionType != null && !exceptionType.isEmpty()) {
            pageResult = exceptionLogRepository.findByExceptionType(exceptionType, pageRequest);
        } else {
            pageResult = exceptionLogRepository.findAll(pageRequest);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ExceptionLog log : pageResult.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("logId", log.getId());
            item.put("userId", log.getUserId());
            item.put("exceptionType", log.getExceptionType());
            item.put("exceptionDetail", log.getExceptionDetail());
            item.put("requestUrl", log.getRequestUrl());
            item.put("ipAddress", log.getIpAddress());
            item.put("createTime", log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : null);
            list.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("list", list);

        return result(200, "获取成功", data);
    }

    @DeleteMapping("/logs/clean")
    public Map<String, Object> cleanLogs(
            @RequestParam(required = false, defaultValue = "all") String logType,
            @RequestParam(required = false) String beforeDate) {

        long deletedCount = 0;
        LocalDateTime beforeDateTime = null;

        // 解析日期参数（清理该日期之前的日志）
        if (beforeDate != null && !beforeDate.isEmpty()) {
            try {
                beforeDateTime = LocalDateTime.parse(beforeDate + " 00:00:00", FORMATTER);
            } catch (Exception e) {
                return result(400, "日期格式错误，请使用 YYYY-MM-DD 格式", null);
            }
        }

        if ("operation".equals(logType) || "all".equals(logType)) {
            if (beforeDateTime != null) {
                deletedCount += operationLogRepository.countByCreateTimeBefore(beforeDateTime);
                operationLogRepository.deleteByCreateTimeBefore(beforeDateTime);
            } else {
                deletedCount += operationLogRepository.count();
                operationLogRepository.deleteAll();
            }
        }

        if ("exception".equals(logType) || "all".equals(logType)) {
            if (beforeDateTime != null) {
                deletedCount += exceptionLogRepository.countByCreateTimeBefore(beforeDateTime);
                exceptionLogRepository.deleteByCreateTimeBefore(beforeDateTime);
            } else {
                deletedCount += exceptionLogRepository.count();
                exceptionLogRepository.deleteAll();
            }
        }

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("clean_logs");
        log.setActionText("清理日志");
        log.setTargetType("config");
        operationLogRepository.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", deletedCount);

        return result(200, "日志清理成功", data);
    }

    // ==================== 配置管理 ====================

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> data = new HashMap<>();

        // 默认配置
        Map<String, Object> health = new HashMap<>();
        health.put("defaultExerciseTarget", 150);

        Map<String, Object> apiWeights = new HashMap<>();
        apiWeights.put("courseHours", 30);
        apiWeights.put("examCount", 15);
        apiWeights.put("ddlCount", 5);
        apiWeights.put("completionRate", 20);

        Map<String, Object> api = new HashMap<>();
        api.put("apiWeights", apiWeights);

        Map<String, Object> system = new HashMap<>();
        system.put("logRetentionDays", 90);
        system.put("abnormalCheckinThreshold", 50);
        system.put("procrastinationThreshold", 70);

        data.put("health", health);
        data.put("api", api);
        data.put("system", system);

        return result(200, "获取成功", data);
    }

    @PutMapping("/config")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> body) {
        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("update_config");
        log.setActionText("更新配置");
        log.setTargetType("config");
        log.setDetails(body.toString());
        operationLogRepository.save(log);

        return result(200, "配置更新成功", null);
    }

    // ==================== 管理员管理 ====================

    @PostMapping("/admins")
    public Map<String, Object> createAdmin(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");
        String nickname = body.get("nickname");

        // 检查手机号是否已存在
        if (userRepository.findByPhone(phone).isPresent()) {
            return result(409, "该手机号已被注册", null);
        }

        User admin = new User();
        admin.setUserId("adm_" + UUID.randomUUID().toString().substring(0, 8));
        admin.setPhone(phone);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNickname(nickname);
        admin.setRole("admin");
        admin.setStatus("normal");
        userRepository.save(admin);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setAdminId("admin_001");
        log.setAdminNickname("管理员");
        log.setAction("create_admin");
        log.setActionText("创建管理员");
        log.setTargetType("user");
        log.setTargetId(admin.getUserId());
        log.setTargetNickname(nickname);
        operationLogRepository.save(log);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", admin.getUserId());
        data.put("role", "admin");

        return result(200, "创建成功", data);
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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String getReasonText(String reasonCode) {
        if (reasonCode == null) return null;
        return switch (reasonCode) {
            case "content_violation" -> "内容违规";
            case "security_risk" -> "安全风险";
            case "abnormal_checkin" -> "异常打卡";
            case "other" -> "其他";
            default -> reasonCode;
        };
    }
}
