package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.analysis.*;
import com.smartcampuslifeserver.entity.ScheduleEvent;
import com.smartcampuslifeserver.entity.SleepRecord;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.repository.*;
import com.smartcampuslifeserver.service.AnalysisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> STRONG_CATEGORIES = Set.of("course", "exam", "activity");

    private final SleepRecordRepository sleepRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final UserRepository userRepository;

    private final int defaultExerciseTarget;
    private final int procrastinationThreshold;

    public AnalysisServiceImpl(SleepRecordRepository sleepRecordRepository,
                               ExerciseRecordRepository exerciseRecordRepository,
                               ScheduleEventRepository scheduleEventRepository,
                               UserRepository userRepository,
                               @Value("${app.report.default-exercise-target:150}") int defaultExerciseTarget,
                               @Value("${app.report.procrastination-threshold:70}") int procrastinationThreshold) {
        this.sleepRecordRepository = sleepRecordRepository;
        this.exerciseRecordRepository = exerciseRecordRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.userRepository = userRepository;
        this.defaultExerciseTarget = defaultExerciseTarget;
        this.procrastinationThreshold = procrastinationThreshold;
    }

    @Override
    public Result<SleepAnalysisResponse> analyzeSleep(String userId) {
        validateUser(userId);
        LocalDate weekStart = currentWeekStart();
        LocalDate weekEnd = currentWeekEnd();

        List<SleepRecord> records = sleepRecordRepository.findByUserIdAndSleepDateBetween(userId, weekStart, weekEnd);
        SleepAnalysisResponse response = new SleepAnalysisResponse();
        SleepAnalysisResponse.Summary summary = new SleepAnalysisResponse.Summary();

        if (records.isEmpty()) {
            summary.setAvgDuration(0.0);
            summary.setAvgQuality("一般");
            response.setSummary(summary);
            response.setAlerts(List.of());
            return Result.success(response, "获取成功");
        }

        double avgDuration = records.stream().mapToDouble(r -> r.getDuration().doubleValue()).average().orElse(0);
        summary.setAvgDuration(Math.round(avgDuration * 10) / 10.0);
        summary.setAvgQuality(mostFrequentQuality(records));

        List<SleepAnalysisResponse.AlertItem> alerts = new ArrayList<>();
        if (avgDuration < 7) {
            SleepAnalysisResponse.AlertItem alert = new SleepAnalysisResponse.AlertItem();
            alert.setType("睡眠不足");
            alert.setSuggestion("本周平均睡眠不足7小时，建议今晚23:30前入睡");
            alerts.add(alert);
        }
        if (records.size() >= 3) {
            long distinctBedHours = records.stream()
                    .map(r -> r.getBedTime().getHour()).distinct().count();
            if (distinctBedHours >= 3) {
                SleepAnalysisResponse.AlertItem alert = new SleepAnalysisResponse.AlertItem();
                alert.setType("作息不规律");
                alert.setSuggestion("近期主睡眠段入睡时间波动大，建议今晚23:30前入睡");
                alerts.add(alert);
            }
        }

        response.setSummary(summary);
        response.setAlerts(alerts);
        return Result.success(response, "获取成功");
    }

    @Override
    public Result<ExerciseAnalysisResponse> analyzeExercise(String userId) {
        validateUser(userId);
        LocalDate weekStart = currentWeekStart();
        LocalDate weekEnd = currentWeekEnd();

        int actualMinutes = exerciseRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd)
                .stream().mapToInt(r -> r.getDuration() * (r.getCount() != null ? r.getCount() : 1)).sum();

        int adjustedTarget = defaultExerciseTarget;
        List<String> reasons = new ArrayList<>();

        User user = userRepository.findByUserId(userId).orElseThrow();
        double bmi = calcBmi(user);
        if (bmi > 24) {
            adjustedTarget += 30;
            reasons.add("BMI>24, 目标增加30分钟");
        }

        PressureAnalysisResponse pressure = calculatePressure(userId, weekStart, weekEnd);
        if (pressure.getApiScore() > 80) {
            adjustedTarget -= 30;
            reasons.add("压力指数>80, 目标减少30分钟");
        }
        adjustedTarget = Math.max(30, adjustedTarget);

        ExerciseAnalysisResponse response = new ExerciseAnalysisResponse();
        response.setDefaultTarget(defaultExerciseTarget);
        response.setAdjustedTarget(adjustedTarget);
        response.setAdjustReason(reasons.isEmpty() ? "无调整" : String.join("; ", reasons));
        response.setActualMinutes(actualMinutes);
        response.setProgress(adjustedTarget == 0 ? 0.0
                : Math.round(actualMinutes * 1000.0 / adjustedTarget) / 10.0);

        if (actualMinutes >= adjustedTarget) {
            response.setStatus("ahead");
        } else if (actualMinutes >= adjustedTarget * 0.7) {
            response.setStatus("normal");
        } else {
            response.setStatus("behind");
        }

        List<String> suggestions = new ArrayList<>();
        if ("behind".equals(response.getStatus())) {
            suggestions.add("运动进度落后，明天下午14:00-16:00没课，去操场跑两圈吧？");
        }
        response.setSuggestions(suggestions);

        return Result.success(response, "获取成功");
    }

    @Override
    public Result<PressureAnalysisResponse> analyzePressure(String userId) {
        validateUser(userId);
        return Result.success(calculatePressure(userId, currentWeekStart(), currentWeekEnd()), "获取成功");
    }

    @Override
    public Result<ProcrastinationAnalysisResponse> analyzeProcrastination(String userId) {
        validateUser(userId);
        LocalDate weekStart = currentWeekStart();
        LocalDate weekEnd = currentWeekEnd();

        List<ScheduleEvent> events = scheduleEventRepository.findByUserId(userId).stream()
                .filter(e -> isInWeek(e, weekStart, weekEnd))
                .filter(e -> "ddl".equals(e.getCategory()) || "personal".equals(e.getCategory()))
                .toList();

        int total = events.size();
        int completed = (int) events.stream().filter(e -> "completed".equals(e.getStatus())).count();
        int weekCompletionRate = total == 0 ? 100 : (int) Math.round(completed * 100.0 / total);

        int onTimeCompleted = (int) events.stream()
                .filter(e -> "completed".equals(e.getStatus()))
                .filter(e -> e.getDeadline() == null || e.getCreateTime().isBefore(e.getDeadline()))
                .count();
        int onTimeRate = completed == 0 ? 100 : (int) Math.round(onTimeCompleted * 100.0 / completed);

        ProcrastinationAnalysisResponse response = new ProcrastinationAnalysisResponse();
        response.setWeekCompletionRate(weekCompletionRate);
        response.setOnTimeRate(onTimeRate);
        response.setThreshold(procrastinationThreshold);

        if (weekCompletionRate < procrastinationThreshold) {
            response.setAlert("极限赶工");
            response.setSuggestion("本周有" + onTimeRate + "%的DDL在截止日当天完成，建议提前规划");
        }

        return Result.success(response, "获取成功");
    }

    @Override
    public Result<SportSuggestionResponse> sportSuggestion(String userId) {
        validateUser(userId);
        SportSuggestionResponse response = new SportSuggestionResponse();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rangeEnd = now.plusDays(2);
        List<ScheduleEvent> busyEvents = scheduleEventRepository.findByUserId(userId).stream()
                .filter(e -> STRONG_CATEGORIES.contains(e.getCategory()))
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .filter(e -> !e.getEndTime().isBefore(now) && !e.getStartTime().isAfter(rangeEnd))
                .sorted(Comparator.comparing(ScheduleEvent::getStartTime))
                .toList();

        List<SportSuggestionResponse.SuggestionItem> suggestions = findFreeSlots(now, rangeEnd, busyEvents);
        response.setHasSuggestion(!suggestions.isEmpty());
        response.setSuggestions(suggestions);
        return Result.success(response, "获取成功");
    }

    @Override
    public PressureAnalysisResponse calculatePressure(String userId, LocalDate weekStart, LocalDate weekEnd) {
        List<ScheduleEvent> weekEvents = scheduleEventRepository.findByUserId(userId).stream()
                .filter(e -> isInWeek(e, weekStart, weekEnd)).toList();

        int courseHours = (int) weekEvents.stream()
                .filter(e -> "course".equals(e.getCategory()))
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .mapToLong(e -> java.time.Duration.between(e.getStartTime(), e.getEndTime()).toHours())
                .sum();

        int examCount = (int) weekEvents.stream().filter(e -> "exam".equals(e.getCategory())).count();
        int ddlCount = (int) weekEvents.stream().filter(e -> "ddl".equals(e.getCategory())).count();

        int total = weekEvents.size();
        int completed = (int) weekEvents.stream().filter(e -> "completed".equals(e.getStatus())).count();
        double completionRate = total == 0 ? 1.0 : (double) completed / total;

        double courseContribution = courseHours / 8.0 * 30;
        double examContribution = examCount * 15.0;
        double ddlContribution = ddlCount * 5.0;
        double completionContribution = (1 - completionRate) * 20;

        int apiScore = (int) Math.round(courseContribution + examContribution + ddlContribution + completionContribution);
        apiScore = Math.min(100, Math.max(0, apiScore));

        PressureAnalysisResponse response = new PressureAnalysisResponse();
        response.setApiScore(apiScore);
        response.setLevel(apiScore < 50 ? "low" : apiScore <= 80 ? "medium" : "high");
        response.setLevelText(apiScore < 50 ? "较低" : apiScore <= 80 ? "适中" : "较高");

        PressureAnalysisResponse.Breakdown breakdown = new PressureAnalysisResponse.Breakdown();
        breakdown.setCourseHours(courseHours);
        breakdown.setCourseContribution(Math.round(courseContribution * 10) / 10.0);
        breakdown.setExamCount(examCount);
        breakdown.setExamContribution(examContribution);
        breakdown.setDdlCount(ddlCount);
        breakdown.setDdlContribution(ddlContribution);
        breakdown.setCompletionRate(Math.round(completionRate * 100) / 100.0);
        breakdown.setCompletionContribution(Math.round(completionContribution * 10) / 10.0);
        response.setBreakdown(breakdown);
        return response;
    }

    private List<SportSuggestionResponse.SuggestionItem> findFreeSlots(
            LocalDateTime start, LocalDateTime end, List<ScheduleEvent> busyEvents) {
        List<SportSuggestionResponse.SuggestionItem> result = new ArrayList<>();
        LocalDateTime cursor = start.withHour(8).withMinute(0);

        while (cursor.isBefore(end)) {
            LocalDateTime dayEnd = cursor.toLocalDate().atTime(22, 0);
            if (dayEnd.isAfter(end)) {
                dayEnd = end;
            }

            LocalDateTime slotStart = cursor;
            for (ScheduleEvent event : busyEvents) {
                if (event.getEndTime().isBefore(slotStart) || event.getStartTime().isAfter(dayEnd)) {
                    continue;
                }
                if (event.getStartTime().isAfter(slotStart)) {
                    long hours = java.time.Duration.between(slotStart, event.getStartTime()).toHours();
                    if (hours >= 2) {
                        result.add(buildSuggestion(slotStart, event.getStartTime(), (int) hours));
                    }
                }
                if (event.getEndTime().isAfter(slotStart)) {
                    slotStart = event.getEndTime();
                }
            }
            if (java.time.Duration.between(slotStart, dayEnd).toHours() >= 2) {
                int hours = (int) java.time.Duration.between(slotStart, dayEnd).toHours();
                result.add(buildSuggestion(slotStart, dayEnd, hours));
            }
            cursor = cursor.toLocalDate().plusDays(1).atTime(8, 0);
        }
        return result.size() > 3 ? result.subList(0, 3) : result;
    }

    private SportSuggestionResponse.SuggestionItem buildSuggestion(LocalDateTime s, LocalDateTime e, int hours) {
        SportSuggestionResponse.SuggestionItem item = new SportSuggestionResponse.SuggestionItem();
        item.setStartTime(s.format(DATE_TIME_FMT));
        item.setEndTime(e.format(DATE_TIME_FMT));
        item.setDuration(hours);
        item.setMessage("运动进度落后，" + s.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                + "-" + e.format(DateTimeFormatter.ofPattern("HH:mm")) + "空闲，去操场跑两圈吧？");
        return item;
    }

    private boolean isInWeek(ScheduleEvent e, LocalDate weekStart, LocalDate weekEnd) {
        LocalDate ref = e.getStartTime() != null ? e.getStartTime().toLocalDate()
                : e.getDeadline() != null ? e.getDeadline().toLocalDate() : null;
        return ref != null && !ref.isBefore(weekStart) && !ref.isAfter(weekEnd);
    }

    private String mostFrequentQuality(List<SleepRecord> records) {
        return records.stream()
                .map(SleepRecord::getQuality)
                .max(Comparator.comparingInt(q -> (int) records.stream().filter(r -> q.equals(r.getQuality())).count()))
                .orElse("一般");
    }

    private double calcBmi(User user) {
        if (user.getHeight() == null || user.getWeight() == null || user.getHeight() == 0) {
            return 22.0;
        }
        double h = user.getHeight() / 100.0;
        return user.getWeight() / (h * h);
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
