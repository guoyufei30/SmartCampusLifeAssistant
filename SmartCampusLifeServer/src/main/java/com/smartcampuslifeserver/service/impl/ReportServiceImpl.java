package com.smartcampuslifeserver.service.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.analysis.PressureAnalysisResponse;
import com.smartcampuslifeserver.dto.report.RegenerateReportRequest;
import com.smartcampuslifeserver.dto.report.WeeklyReportHistoryItem;
import com.smartcampuslifeserver.dto.report.WeeklyReportResponse;
import com.smartcampuslifeserver.entity.*;
import com.smartcampuslifeserver.repository.*;
import com.smartcampuslifeserver.service.AnalysisService;
import com.smartcampuslifeserver.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_WEEKS = 12;

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final AnalysisService analysisService;
    private final ObjectMapper objectMapper;

    public ReportServiceImpl(WeeklyReportRepository weeklyReportRepository,
                             UserRepository userRepository,
                             ScheduleEventRepository scheduleEventRepository,
                             SleepRecordRepository sleepRecordRepository,
                             ExerciseRecordRepository exerciseRecordRepository,
                             WeightRecordRepository weightRecordRepository,
                             AnalysisService analysisService,
                             ObjectMapper objectMapper) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.sleepRecordRepository = sleepRecordRepository;
        this.exerciseRecordRepository = exerciseRecordRepository;
        this.weightRecordRepository = weightRecordRepository;
        this.analysisService = analysisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<WeeklyReportResponse> getCurrentWeeklyReport(String userId) {
        validateUser(userId);
        int weekNumber = LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        LocalDate weekStart = currentWeekStart();
        LocalDate weekEnd = currentWeekEnd();

        WeeklyReport report = weeklyReportRepository.findByUserIdAndWeekNumber(userId, weekNumber)
                .orElseGet(() -> generateAndSave(userId, weekNumber, weekStart, weekEnd));

        return Result.success(toResponse(report, true), "获取成功");
    }

    @Override
    public Result<List<WeeklyReportHistoryItem>> getHistory(String userId) {
        validateUser(userId);
        List<WeeklyReportHistoryItem> list = weeklyReportRepository.findByUserIdOrderByWeekNumberDesc(userId)
                .stream().limit(MAX_WEEKS).map(r -> {
                    WeeklyReportHistoryItem item = new WeeklyReportHistoryItem();
                    item.setId(r.getId());
                    item.setWeekNumber(r.getWeekNumber());
                    item.setStartDate(r.getStartDate().format(DATE_FMT));
                    item.setEndDate(r.getEndDate().format(DATE_FMT));
                    return item;
                }).toList();
        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<WeeklyReportResponse> regenerate(String userId, RegenerateReportRequest request) {
        validateUser(userId);
        int targetWeek = request.getWeekNumber() != null
                ? request.getWeekNumber()
                : LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());

        int currentWeek = LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        if (targetWeek != currentWeek) {
            throw new BusinessException(400, "只能重新生成最近一周的周报");
        }

        WeeklyReport existing = weeklyReportRepository.findByUserIdAndWeekNumber(userId, targetWeek)
                .orElseThrow(() -> new BusinessException(404, "周报不存在"));

        if (existing.getRegenerateAvailableUntil() == null
                || LocalDateTime.now().isAfter(existing.getRegenerateAvailableUntil())) {
            throw new BusinessException(400, "超出时间窗口，不可重新生成");
        }

        LocalDate weekStart = existing.getStartDate();
        LocalDate weekEnd = existing.getEndDate();
        WeeklyReport updated = buildReportEntity(userId, targetWeek, weekStart, weekEnd);
        updated.setId(existing.getId());
        updated.setCreateTime(existing.getCreateTime());
        weeklyReportRepository.save(updated);

        return Result.success(toResponse(updated, true), "重新生成成功");
    }

    @Override
    public Result<WeeklyReportResponse> getDetail(String userId, Long reportId) {
        validateUser(userId);
        WeeklyReport report = weeklyReportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new BusinessException(404, "周报不存在"));
        boolean isCurrent = report.getWeekNumber().equals(
                LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
        return Result.success(toResponse(report, isCurrent), "获取成功");
    }

    @Override
    @Transactional
    public void generateWeeklyReportsForAllUsers() {
        int weekNumber = LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
        LocalDate weekStart = currentWeekStart();
        LocalDate weekEnd = currentWeekEnd();

        for (User user : userRepository.findAll()) {
            if (!"user".equals(user.getRole())) {
                continue;
            }
            weeklyReportRepository.findByUserIdAndWeekNumber(user.getUserId(), weekNumber)
                    .ifPresentOrElse(r -> {
                    }, () -> generateAndSave(user.getUserId(), weekNumber, weekStart, weekEnd));
        }
        pruneOldReports();
    }

    private WeeklyReport generateAndSave(String userId, int weekNumber, LocalDate weekStart, LocalDate weekEnd) {
        WeeklyReport report = buildReportEntity(userId, weekNumber, weekStart, weekEnd);
        return weeklyReportRepository.save(report);
    }

    private WeeklyReport buildReportEntity(String userId, int weekNumber, LocalDate weekStart, LocalDate weekEnd) {
        Map<String, Object> sections = new LinkedHashMap<>();

        List<ScheduleEvent> events = scheduleEventRepository.findByUserId(userId).stream()
                .filter(e -> isInWeek(e, weekStart, weekEnd)).toList();
        Map<String, Object> schedule = new LinkedHashMap<>();
        schedule.put("totalEvents", events.size());
        schedule.put("completed", events.stream().filter(e -> "completed".equals(e.getStatus())).count());
        schedule.put("overdue", events.stream().filter(e -> "overdue".equals(e.getStatus())).count());
        sections.put("schedule", schedule);

        List<SleepRecord> sleepRecords = sleepRecordRepository.findByUserIdAndSleepDateBetween(userId, weekStart, weekEnd);
        Map<String, Object> sleep = new LinkedHashMap<>();
        double avgDuration = sleepRecords.stream().mapToDouble(r -> r.getDuration().doubleValue()).average().orElse(0);
        sleep.put("avgDuration", Math.round(avgDuration * 10) / 10.0);
        sleep.put("avgQuality", sleepRecords.isEmpty() ? "一般" : sleepRecords.get(0).getQuality());
        sections.put("sleep", sleep);

        int totalMinutes = exerciseRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd)
                .stream().mapToInt(r -> r.getDuration() * (r.getCount() != null ? r.getCount() : 1)).sum();
        Map<String, Object> exercise = new LinkedHashMap<>();
        exercise.put("totalMinutes", totalMinutes);
        exercise.put("targetMet", totalMinutes >= 150);
        sections.put("exercise", exercise);

        List<WeightRecord> weights = weightRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);
        Map<String, Object> weight = new LinkedHashMap<>();
        if (!weights.isEmpty()) {
            weight.put("startWeight", weights.get(0).getWeight().doubleValue());
            weight.put("endWeight", weights.get(weights.size() - 1).getWeight().doubleValue());
        }
        sections.put("weight", weight);

        PressureAnalysisResponse pressure = analysisService.calculatePressure(userId, weekStart, weekEnd);
        Map<String, Object> pressureMap = new LinkedHashMap<>();
        pressureMap.put("avgScore", pressure.getApiScore());
        pressureMap.put("trend", "stable");
        sections.put("pressure", pressureMap);

        List<String> suggestions = new ArrayList<>();
        if (Boolean.TRUE.equals(exercise.get("targetMet"))) {
            suggestions.add("本周运动目标已达成，继续保持！");
        } else {
            suggestions.add("本周运动未达标，下周适当增加运动时间。");
        }

        WeeklyReport report = new WeeklyReport();
        report.setUserId(userId);
        report.setWeekNumber(weekNumber);
        report.setStartDate(weekStart);
        report.setEndDate(weekEnd);
        try {
            report.setSections(objectMapper.writeValueAsString(sections));
            report.setSuggestions(objectMapper.writeValueAsString(suggestions));
        } catch (Exception e) {
            throw new BusinessException(500, "周报数据序列化失败");
        }
        report.setRegenerateAvailableUntil(LocalDateTime.now().plusHours(24));
        return report;
    }

    private WeeklyReportResponse toResponse(WeeklyReport report, boolean isCurrentWeek) {
        WeeklyReportResponse response = new WeeklyReportResponse();
        response.setId(report.getId());
        response.setWeekNumber(report.getWeekNumber());
        response.setStartDate(report.getStartDate().format(DATE_FMT));
        response.setEndDate(report.getEndDate().format(DATE_FMT));

        boolean canRegenerate = isCurrentWeek && report.getRegenerateAvailableUntil() != null
                && LocalDateTime.now().isBefore(report.getRegenerateAvailableUntil());
        response.setCanRegenerate(canRegenerate);
        if (report.getRegenerateAvailableUntil() != null) {
            response.setRegenerateAvailableUntil(report.getRegenerateAvailableUntil().toLocalDate().format(DATE_FMT));
        }

        try {
            if (report.getSections() != null) {
                response.setSections(objectMapper.readValue(report.getSections(),
                        new TypeReference<Map<String, Object>>() {}));
            }
            if (report.getSuggestions() != null) {
                response.setSuggestions(objectMapper.readValue(report.getSuggestions(),
                        new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) {
            response.setSections(Map.of());
            response.setSuggestions(List.of());
        }
        return response;
    }

    private void pruneOldReports() {
        for (User user : userRepository.findAll()) {
            List<WeeklyReport> reports = weeklyReportRepository.findByUserIdOrderByWeekNumberDesc(user.getUserId());
            if (reports.size() > MAX_WEEKS) {
                reports.subList(MAX_WEEKS, reports.size()).forEach(weeklyReportRepository::delete);
            }
        }
    }

    private boolean isInWeek(ScheduleEvent e, LocalDate weekStart, LocalDate weekEnd) {
        LocalDate ref = e.getStartTime() != null ? e.getStartTime().toLocalDate()
                : e.getDeadline() != null ? e.getDeadline().toLocalDate() : null;
        return ref != null && !ref.isBefore(weekStart) && !ref.isAfter(weekEnd);
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate currentWeekEnd() {
        return currentWeekStart().plusDays(6);
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
}
