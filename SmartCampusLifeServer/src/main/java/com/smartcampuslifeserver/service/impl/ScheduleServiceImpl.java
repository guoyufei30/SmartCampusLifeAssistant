package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.*;
import com.smartcampuslifeserver.entity.Course;
import com.smartcampuslifeserver.entity.ExceptionLog;
import com.smartcampuslifeserver.entity.ScheduleEvent;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.repository.*;
import com.smartcampuslifeserver.service.ScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> STRONG_CATEGORIES = Set.of("course", "exam", "activity");
    private static final Set<String> ALL_CATEGORIES = Set.of("course", "exam", "activity", "ddl", "personal");
    private static final Set<String> VIEW_TYPES = Set.of("day", "week", "month");
    private static final Set<String> REMINDERS = Set.of("none", "15min", "1hour", "1day");
    private static final List<String> SENSITIVE_WORDS = List.of(
            "违禁", "管理员", "暴力", "色情", "赌博", "fuck", "admin", "傻逼", "毒品"
    );

    private final ScheduleEventRepository scheduleEventRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final ExceptionLogRepository exceptionLogRepository;

    public ScheduleServiceImpl(ScheduleEventRepository scheduleEventRepository,
                               CourseRepository courseRepository,
                               SemesterRepository semesterRepository,
                               UserRepository userRepository,
                               ExceptionLogRepository exceptionLogRepository) {
        this.scheduleEventRepository = scheduleEventRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.exceptionLogRepository = exceptionLogRepository;
    }

    @Override
    @Transactional
    public Result<List<ScheduleEventResponse>> listEvents(String userId, String startDate, String endDate,
                                                          String viewType, String category) {
        validateUser(userId);
        LocalDate anchorDate = parseDate(startDate, "startDate");
        LocalDate parsedEnd = endDate != null && !endDate.isBlank()
                ? parseDate(endDate, "endDate") : null;
        String normalizedCategory = normalizeCategoryFilter(category);
        DateRange queryRange = resolveQueryRange(anchorDate, parsedEnd, viewType);

        LocalDateTime rangeStart = queryRange.start().atStartOfDay();
        LocalDateTime rangeEnd = queryRange.end().atTime(23, 59, 59);

        List<ScheduleEvent> events = scheduleEventRepository.findByUserId(userId);
        List<ScheduleEventResponse> responses = new ArrayList<>();

        for (ScheduleEvent event : events) {
            if (normalizedCategory != null && !normalizedCategory.equals(event.getCategory())) {
                continue;
            }
            if (!isEventInRange(event, rangeStart, rangeEnd)) {
                continue;
            }
            refreshAutoStatus(event);
            responses.add(toResponse(event));
        }

        responses.sort(Comparator.comparing(e -> e.getStartTime() != null ? e.getStartTime() : e.getDeadline(),
                Comparator.nullsLast(String::compareTo)));

        return Result.success(responses, "获取成功");
    }

    @Override
    @Transactional
    public Result<CreateEventResponse> createEvent(String userId, CreateEventRequest request) {
        validateUser(userId);
        validateEventRequest(request.getTitle(), request.getCategory(), request.getStartTime(),
                request.getEndTime(), request.getDeadline(), request.getReminder());
        checkSensitiveRemark(userId, request.getRemark(), "/schedule/events");

        LocalDateTime start = parseDateTime(request.getStartTime());
        LocalDateTime end = parseDateTime(request.getEndTime());
        LocalDateTime deadline = parseDateTime(request.getDeadline());
        validateCategoryTimeRules(request.getCategory(), start, end, deadline);

        if (isStrongCategory(request.getCategory())) {
            ConflictCheckResponse conflict = detectConflict(userId, request.getCategory(), start, end, null);
            if (Boolean.TRUE.equals(conflict.getHasConflict())) {
                String title = conflict.getConflictEvent().getTitle();
                throw new BusinessException(409,
                        "添加失败：您在该时间段已有[" + title + "]安排，存在强冲突，请修改时间。");
            }
        }
        if (request.getLocation() != null && request.getLocation().length() > 50) {
            throw new BusinessException(400, "地点不能超过50个字符");
        }

        ScheduleEvent event = new ScheduleEvent();
        event.setUserId(userId);
        applyEventFields(event, request.getTitle(), request.getCategory(), start, end, deadline,
                request.getLocation(), request.getRemark(), request.getReminder());
        event.setStatus(computeAutoStatus(event, LocalDateTime.now()));

        if ("course".equals(request.getCategory())) {
            Course course = createLinkedCourse(userId, request.getTitle(), start, end, request.getLocation());
            courseRepository.save(course);
            event.setCourseId(course.getId());
        }

        scheduleEventRepository.save(event);

        CreateEventResponse response = new CreateEventResponse();
        response.setId(event.getId());
        return Result.success(response, "创建成功");
    }

    @Override
    @Transactional
    public Result<Void> updateEvent(String userId, String eventId, UpdateEventRequest request) {
        validateUser(userId);
        ScheduleEvent event = scheduleEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(404, "日程不存在"));

        String title = request.getTitle() != null ? request.getTitle() : event.getTitle();
        String category = request.getCategory() != null ? request.getCategory() : event.getCategory();
        String startStr = request.getStartTime() != null ? request.getStartTime()
                : formatDateTime(event.getStartTime());
        String endStr = request.getEndTime() != null ? request.getEndTime()
                : formatDateTime(event.getEndTime());
        String deadlineStr = request.getDeadline() != null ? request.getDeadline()
                : formatDateTime(event.getDeadline());
        String reminder = request.getReminder() != null ? request.getReminder() : event.getReminder();

        validateEventRequest(title, category, startStr, endStr, deadlineStr, reminder);
        if (request.getRemark() != null) {
            checkSensitiveRemark(userId, request.getRemark(), "/schedule/events/" + eventId);
        }

        LocalDateTime start = parseDateTime(startStr);
        LocalDateTime end = parseDateTime(endStr);
        LocalDateTime deadline = parseDateTime(deadlineStr);
        validateCategoryTimeRules(category, start, end, deadline);

        if (isStrongCategory(category)) {
            ConflictCheckResponse conflict = detectConflict(userId, category, start, end, eventId);
            if (Boolean.TRUE.equals(conflict.getHasConflict())) {
                String conflictTitle = conflict.getConflictEvent().getTitle();
                throw new BusinessException(409,
                        "修改失败：您在该时间段已有[" + conflictTitle + "]安排，存在强冲突，请修改时间。");
            }
        }

        applyEventFields(event, title, category, start, end, deadline,
                request.getLocation() != null ? request.getLocation() : event.getLocation(),
                request.getRemark() != null ? request.getRemark() : event.getRemark(),
                reminder);
        if (!"completed".equals(event.getStatus())) {
            event.setStatus(computeAutoStatus(event, LocalDateTime.now()));
        }
        event.setReminderAcked(false);

        if ("course".equals(category)) {
            if (event.getCourseId() == null) {
                Course course = createLinkedCourse(userId, title, start, end, event.getLocation());
                courseRepository.save(course);
                event.setCourseId(course.getId());
            } else {
                updateLinkedCourse(event.getCourseId(), userId, title, start, end, event.getLocation());
            }
        } else if (event.getCourseId() != null) {
            courseRepository.deleteById(event.getCourseId());
            event.setCourseId(null);
        }

        scheduleEventRepository.save(event);
        return Result.success(null, "更新成功");
    }

    @Override
    @Transactional
    public Result<Void> deleteEvent(String userId, String eventId) {
        validateUser(userId);
        ScheduleEvent event = scheduleEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(404, "日程不存在"));

        if (event.getCourseId() != null) {
            courseRepository.deleteById(event.getCourseId());
        }
        scheduleEventRepository.delete(event);
        return Result.success(null, "删除成功");
    }

    @Override
    @Transactional
    public Result<Void> updateStatus(String userId, String eventId, UpdateEventStatusRequest request) {
        validateUser(userId);
        if (request.getStatus() == null
                || (!"pending".equals(request.getStatus()) && !"completed".equals(request.getStatus()))) {
            throw new BusinessException(400, "状态仅支持 pending 或 completed");
        }

        ScheduleEvent event = scheduleEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(404, "日程不存在"));

        event.setStatus(request.getStatus());
        scheduleEventRepository.save(event);
        return Result.success(null, "状态更新成功");
    }

    @Override
    public Result<ConflictCheckResponse> checkConflict(String userId, ConflictCheckRequest request) {
        validateUser(userId);
        if (request.getCategory() == null || !ALL_CATEGORIES.contains(request.getCategory())) {
            throw new BusinessException(400, "日程分类无效");
        }

        ConflictCheckResponse response = new ConflictCheckResponse();
        if (!isStrongCategory(request.getCategory())) {
            response.setHasConflict(false);
            return Result.success(response, "无冲突");
        }

        LocalDateTime start = parseDateTimeRequired(request.getStartTime(), "startTime");
        LocalDateTime end = parseDateTimeRequired(request.getEndTime(), "endTime");
        if (!end.isAfter(start)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }

        response = detectConflict(userId, request.getCategory(), start, end, request.getExcludeId());
        if (Boolean.TRUE.equals(response.getHasConflict())) {
            String title = response.getConflictEvent().getTitle();
            return Result.of(409,
                    "添加失败：您在该时间段已有[" + title + "]安排，存在强冲突，请修改时间。",
                    response);
        }
        response.setHasConflict(false);
        return Result.success(response, "无冲突");
    }

    @Override
    @Transactional
    public Result<List<ReminderResponse>> listReminders(String userId) {
        validateUser(userId);
        LocalDateTime now = LocalDateTime.now();
        List<ReminderResponse> reminders = new ArrayList<>();

        for (ScheduleEvent event : scheduleEventRepository.findByUserId(userId)) {
            if (Boolean.TRUE.equals(event.getReminderAcked())) {
                continue;
            }
            if ("completed".equals(event.getStatus())) {
                continue;
            }
            String reminder = event.getReminder();
            if (reminder == null || "none".equals(reminder)) {
                continue;
            }

            LocalDateTime triggerTime = resolveReminderTime(event);
            if (triggerTime == null || now.isBefore(triggerTime)) {
                continue;
            }

            ReminderResponse item = new ReminderResponse();
            item.setId(event.getId());
            item.setTitle(event.getTitle());
            item.setReminder(reminder);
            item.setReminderTime(triggerTime.format(DATE_TIME_FMT));
            reminders.add(item);
        }

        return Result.success(reminders, "获取成功");
    }

    @Override
    @Transactional
    public Result<Void> ackReminder(String userId, ReminderAckRequest request) {
        validateUser(userId);
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            throw new BusinessException(400, "eventId 不能为空");
        }

        ScheduleEvent event = scheduleEventRepository.findByIdAndUserId(request.getEventId(), userId)
                .orElseThrow(() -> new BusinessException(404, "日程不存在"));

        event.setReminderAcked(true);
        scheduleEventRepository.save(event);
        return Result.success(null, "确认成功");
    }

    // ==================== 冲突检测核心算法 ====================

    /**
     * 强冲突类（course/exam/activity）之间时间互斥，并检测与课表课程的节次冲突。
     * ddl/personal 不参与冲突检测。
     */
    private ConflictCheckResponse detectConflict(String userId, String category,
                                                 LocalDateTime start, LocalDateTime end,
                                                 String excludeId) {
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setHasConflict(false);

        if (!isStrongCategory(category) || start == null || end == null) {
            return response;
        }

        List<ScheduleEvent> events = scheduleEventRepository.findByUserId(userId);
        for (ScheduleEvent existing : events) {
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            if (!isStrongCategory(existing.getCategory())) {
                continue;
            }
            if (existing.getStartTime() == null || existing.getEndTime() == null) {
                continue;
            }
            if (isTimeOverlap(start, end, existing.getStartTime(), existing.getEndTime())) {
                return buildConflictResponse(existing);
            }
        }

        int eventDayOfWeek = start.getDayOfWeek().getValue();
        int eventStartPeriod = timeToPeriod(start.toLocalTime());
        int eventEndPeriod = timeToPeriod(end.toLocalTime());

        String excludedCourseId = null;
        if (excludeId != null) {
            excludedCourseId = scheduleEventRepository.findById(excludeId)
                    .map(ScheduleEvent::getCourseId)
                    .orElse(null);
        }

        for (Course course : courseRepository.findByUserId(userId)) {
            if (course.getId().equals(excludedCourseId)) {
                continue;
            }
            if (!course.getDayOfWeek().equals(eventDayOfWeek)) {
                continue;
            }
            if (isPeriodOverlap(eventStartPeriod, eventEndPeriod,
                    course.getStartPeriod(), course.getEndPeriod())) {
                ConflictCheckResponse conflict = new ConflictCheckResponse();
                conflict.setHasConflict(true);
                ConflictCheckResponse.ConflictEventInfo info = new ConflictCheckResponse.ConflictEventInfo();
                info.setId(course.getId());
                info.setTitle(course.getName());
                info.setCategory("course");
                conflict.setConflictEvent(info);
                return conflict;
            }
        }

        return response;
    }

    private ConflictCheckResponse buildConflictResponse(ScheduleEvent existing) {
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setHasConflict(true);
        ConflictCheckResponse.ConflictEventInfo info = new ConflictCheckResponse.ConflictEventInfo();
        info.setId(existing.getId());
        info.setTitle(existing.getTitle());
        info.setCategory(existing.getCategory());
        response.setConflictEvent(info);
        return response;
    }

    private boolean isTimeOverlap(LocalDateTime s1, LocalDateTime e1,
                                  LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private boolean isPeriodOverlap(int start1, int end1, int start2, int end2) {
        return start1 <= end2 && start2 <= end1;
    }

    // ==================== 状态自动计算 ====================

    private void refreshAutoStatus(ScheduleEvent event) {
        if ("completed".equals(event.getStatus())) {
            return;
        }
        String computed = computeAutoStatus(event, LocalDateTime.now());
        if (!computed.equals(event.getStatus())) {
            event.setStatus(computed);
            scheduleEventRepository.save(event);
        }
    }

    private String computeAutoStatus(ScheduleEvent event, LocalDateTime now) {
        if ("completed".equals(event.getStatus())) {
            return "completed";
        }
        if ("ddl".equals(event.getCategory()) && event.getDeadline() != null) {
            return now.isAfter(event.getDeadline()) ? "overdue" : "pending";
        }
        if (event.getStartTime() != null && event.getEndTime() != null) {
            if (now.isBefore(event.getStartTime())) {
                return "pending";
            }
            if (!now.isAfter(event.getEndTime())) {
                return "in_progress";
            }
            return "overdue";
        }
        return event.getStatus() != null ? event.getStatus() : "pending";
    }

    // ==================== 课程关联 ====================

    private Course createLinkedCourse(String userId, String title, LocalDateTime start,
                                      LocalDateTime end, String location) {
        Course course = new Course();
        course.setUserId(userId);
        course.setName(title);
        course.setWeekPattern("1-16周");
        course.setDayOfWeek(start.getDayOfWeek().getValue());
        course.setStartPeriod(timeToPeriod(start.toLocalTime()));
        course.setEndPeriod(timeToPeriod(end.toLocalTime()));
        course.setLocation(location);
        course.setSemesterId(resolveCurrentSemesterId());
        return course;
    }

    private void updateLinkedCourse(String courseId, String userId, String title,
                                    LocalDateTime start, LocalDateTime end, String location) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException(404, "关联课程不存在"));
        course.setName(title);
        course.setDayOfWeek(start.getDayOfWeek().getValue());
        course.setStartPeriod(timeToPeriod(start.toLocalTime()));
        course.setEndPeriod(timeToPeriod(end.toLocalTime()));
        course.setLocation(location);
        courseRepository.save(course);
    }

    private Long resolveCurrentSemesterId() {
        return semesterRepository.findByIsCurrentTrue()
                .map(s -> s.getId())
                .orElse(1L);
    }

    // ==================== 节次与时间换算 ====================

    private int timeToPeriod(LocalTime time) {
        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes < 8 * 60 + 55) return 1;
        if (minutes < 10 * 60) return 2;
        if (minutes < 10 * 60 + 55) return 3;
        if (minutes < 12 * 60) return 4;
        if (minutes < 14 * 60 + 55) return 5;
        if (minutes < 16 * 60) return 6;
        if (minutes < 16 * 60 + 55) return 7;
        if (minutes < 18 * 60) return 8;
        if (minutes < 19 * 60 + 55) return 9;
        return 10;
    }

    // ==================== 辅助方法 ====================

    private ScheduleEventResponse toResponse(ScheduleEvent event) {
        ScheduleEventResponse response = new ScheduleEventResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setCategory(event.getCategory());
        response.setStartTime(formatDateTime(event.getStartTime()));
        response.setEndTime(formatDateTime(event.getEndTime()));
        response.setDeadline(formatDateTime(event.getDeadline()));
        response.setLocation(event.getLocation());
        response.setRemark(event.getRemark());
        response.setReminder(event.getReminder());
        response.setStatus(event.getStatus());
        response.setCountdown(buildCountdown(event));

        if (event.getCourseId() != null) {
            courseRepository.findById(event.getCourseId()).ifPresent(course -> {
                response.setWeekPattern(course.getWeekPattern());
                response.setDayOfWeek(course.getDayOfWeek());
                response.setStartPeriod(course.getStartPeriod());
                response.setEndPeriod(course.getEndPeriod());
            });
        }
        return response;
    }

    private String buildCountdown(ScheduleEvent event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = "ddl".equals(event.getCategory())
                ? event.getDeadline() : event.getStartTime();
        if (target == null || !now.isBefore(target)) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), target.toLocalDate());
        if (days > 0) {
            return days + "天";
        }
        long hours = Duration.between(now, target).toHours();
        if (hours > 0) {
            return hours + "小时";
        }
        return "不足1小时";
    }

    private LocalDateTime resolveReminderTime(ScheduleEvent event) {
        LocalDateTime base = "ddl".equals(event.getCategory())
                ? event.getDeadline() : event.getStartTime();
        if (base == null || event.getReminder() == null) {
            return null;
        }
        return switch (event.getReminder()) {
            case "15min" -> base.minusMinutes(15);
            case "1hour" -> base.minusHours(1);
            case "1day" -> base.minusDays(1);
            default -> null;
        };
    }

    private boolean isEventInRange(ScheduleEvent event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (event.getStartTime() != null) {
            return !event.getStartTime().isAfter(rangeEnd)
                    && (event.getEndTime() == null || !event.getEndTime().isBefore(rangeStart));
        }
        if (event.getDeadline() != null) {
            return !event.getDeadline().isBefore(rangeStart) && !event.getDeadline().isAfter(rangeEnd);
        }
        return true;
    }

    private void applyEventFields(ScheduleEvent event, String title, String category,
                                  LocalDateTime start, LocalDateTime end, LocalDateTime deadline,
                                  String location, String remark, String reminder) {
        event.setTitle(title);
        event.setCategory(category);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setDeadline(deadline);
        event.setLocation(location);
        event.setRemark(remark);
        event.setReminder(reminder != null ? reminder : "none");
    }

    private void validateEventRequest(String title, String category, String startTime,
                                      String endTime, String deadline, String reminder) {
        if (title == null || !title.matches("^.{1,20}$")) {
            throw new BusinessException(400, "标题须为1-20个字符");
        }
        if (category == null || !ALL_CATEGORIES.contains(category)) {
            throw new BusinessException(400, "日程分类无效");
        }
        if (reminder != null && !REMINDERS.contains(reminder)) {
            throw new BusinessException(400, "提醒设置无效");
        }
    }

    private void validateCategoryTimeRules(String category, LocalDateTime start,
                                           LocalDateTime end, LocalDateTime deadline) {
        if ("ddl".equals(category)) {
            if (deadline == null) {
                throw new BusinessException(400, "DDL 类日程必须填写 deadline");
            }
            return;
        }
        if (start == null || end == null) {
            throw new BusinessException(400, "开始时间和结束时间不能为空");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
    }

    private void validateUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (Boolean.TRUE.equals(user.getForceChangePassword())) {
            throw new BusinessException(403, "请先修改临时密码", "temp_password_required");
        }
        if ("frozen".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被冻结", "account_frozen");
        }
    }

    private void checkSensitiveRemark(String userId, String remark, String requestUrl) {
        if (remark == null || remark.isBlank()) {
            return;
        }
        if (remark.length() > 200) {
            throw new BusinessException(400, "备注不能超过200个字符");
        }
        String lower = remark.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                ExceptionLog log = new ExceptionLog();
                log.setUserId(userId);
                log.setExceptionType("content_violation");
                log.setExceptionDetail("日程备注包含违禁词：" + word);
                log.setRequestUrl(requestUrl);
                exceptionLogRepository.save(log);
                throw new BusinessException(400, "内容包含违禁词汇，请修改后重试");
            }
        }
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    /**
     * 指定 viewType 时以 startDate 为锚点计算查询范围（忽略 endDate）；
     * 未指定 viewType 时使用 startDate～endDate 自定义区间。
     */
    private DateRange resolveQueryRange(LocalDate startDate, LocalDate endDate, String viewType) {
        if (viewType != null && !viewType.isBlank()) {
            String type = viewType.trim().toLowerCase();
            if (!VIEW_TYPES.contains(type)) {
                throw new BusinessException(400, "viewType 仅支持 day / week / month");
            }
            return switch (type) {
                case "day" -> new DateRange(startDate, startDate);
                case "week" -> new DateRange(
                        startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
                case "month" -> new DateRange(
                        startDate.withDayOfMonth(1),
                        startDate.with(TemporalAdjusters.lastDayOfMonth()));
                default -> throw new BusinessException(400, "viewType 仅支持 day / week / month");
            };
        }
        if (endDate == null) {
            throw new BusinessException(400, "未指定 viewType 时 endDate 不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
        return new DateRange(startDate, endDate);
    }

    private String normalizeCategoryFilter(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim().toLowerCase();
        if (!ALL_CATEGORIES.contains(normalized)) {
            throw new BusinessException(400,
                    "category 无效，仅支持 course / exam / activity / ddl / personal");
        }
        return normalized;
    }

    private boolean isStrongCategory(String category) {
        return category != null && STRONG_CATEGORIES.contains(category);
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, field + " 不能为空");
        }
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, field + " 格式应为 YYYY-MM-DD");
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, "时间格式应为 YYYY-MM-DD HH:mm");
        }
    }

    private LocalDateTime parseDateTimeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, field + " 不能为空");
        }
        return parseDateTime(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FMT) : null;
    }
}
