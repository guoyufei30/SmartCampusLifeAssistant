package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.health.*;
import com.smartcampuslifeserver.entity.*;
import com.smartcampuslifeserver.repository.*;
import com.smartcampuslifeserver.service.HealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthServiceImpl implements HealthService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CREATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> QUALITIES = Set.of("极佳", "较好", "一般", "较差");

    private final SleepRecordRepository sleepRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final UserRepository userRepository;
    private final ExceptionLogRepository exceptionLogRepository;
    private final OperationLogRepository operationLogRepository;

    private final int abnormalCheckinThreshold;
    private final Map<String, List<Long>> checkinCounters = new ConcurrentHashMap<>();

    public HealthServiceImpl(SleepRecordRepository sleepRecordRepository,
                             ExerciseRecordRepository exerciseRecordRepository,
                             WeightRecordRepository weightRecordRepository,
                             UserRepository userRepository,
                             ExceptionLogRepository exceptionLogRepository,
                             OperationLogRepository operationLogRepository,
                             @Value("${app.health.abnormal-checkin-threshold:50}") int abnormalCheckinThreshold) {
        this.sleepRecordRepository = sleepRecordRepository;
        this.exerciseRecordRepository = exerciseRecordRepository;
        this.weightRecordRepository = weightRecordRepository;
        this.userRepository = userRepository;
        this.exceptionLogRepository = exceptionLogRepository;
        this.operationLogRepository = operationLogRepository;
        this.abnormalCheckinThreshold = abnormalCheckinThreshold;
    }

    @Override
    public Result<List<SleepRecordResponse>> listSleep(String userId, String startDate, String endDate) {
        validateUser(userId);
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        List<SleepRecordResponse> list = sleepRecordRepository.findByUserIdAndSleepDateBetween(userId, start, end)
                .stream().map(this::toSleepResponse).toList();
        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<CreateSleepResponse> createSleep(String userId, CreateSleepRequest request) {
        validateUser(userId);
        checkAbnormalCheckin(userId, "/health/sleep");

        LocalDateTime bedTime = parseDateTimeRequired(request.getBedTime(), "bedTime");
        LocalDateTime wakeTime = parseDateTimeRequired(request.getWakeTime(), "wakeTime");
        if (!wakeTime.isAfter(bedTime)) {
            throw new BusinessException(400, "起床时间必须晚于入睡时间");
        }
        if (request.getQuality() == null || !QUALITIES.contains(request.getQuality())) {
            throw new BusinessException(400, "睡眠质量参数无效");
        }

        LocalDate sleepDate = request.getRecordDate() != null && !request.getRecordDate().isBlank()
                ? parseDate(request.getRecordDate(), "recordDate")
                : wakeTime.toLocalDate();
        validateBackfillDate(sleepDate);

        double durationHours = ChronoUnit.MINUTES.between(bedTime, wakeTime) / 60.0;
        durationHours = BigDecimal.valueOf(durationHours).setScale(2, RoundingMode.HALF_UP).doubleValue();

        if ((durationHours < 1 || durationHours > 18) && !Boolean.TRUE.equals(request.getConfirmAbnormal())) {
            CreateSleepResponse response = new CreateSleepResponse();
            response.setRequiresConfirm(true);
            response.setDuration(durationHours);
            return Result.success(response, "检测到睡眠时间异常，是否确认提交？");
        }

        if (hasSleepOverlap(userId, bedTime, wakeTime, null)) {
            throw new BusinessException(409, "录入失败，该时间段与已有睡眠记录冲突，请检查后再试。");
        }

        SleepRecord record = new SleepRecord();
        record.setUserId(userId);
        record.setSleepDate(sleepDate);
        record.setBedTime(bedTime);
        record.setWakeTime(wakeTime);
        record.setDuration(BigDecimal.valueOf(durationHours));
        record.setQuality(request.getQuality());
        sleepRecordRepository.save(record);

        CreateSleepResponse response = new CreateSleepResponse();
        response.setRecord(toSleepResponse(record));
        return Result.success(response, "提交成功");
    }

    @Override
    @Transactional
    public Result<Void> deleteSleep(String userId, String recordId) {
        validateUser(userId);
        SleepRecord record = sleepRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(404, "记录不存在"));
        sleepRecordRepository.delete(record);
        return Result.success(null, "删除成功");
    }

    @Override
    public Result<List<ExerciseRecordResponse>> listExercise(String userId, String startDate, String endDate) {
        validateUser(userId);
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        List<ExerciseRecordResponse> list = exerciseRecordRepository.findByUserIdAndRecordDateBetween(userId, start, end)
                .stream().map(this::toExerciseResponse).toList();
        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<ExerciseRecordResponse> createExercise(String userId, CreateExerciseRequest request) {
        validateUser(userId);
        checkAbnormalCheckin(userId, "/health/exercise");

        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException(400, "运动类型不能为空");
        }
        if (request.getDuration() == null || request.getDuration() < 1 || request.getDuration() > 600) {
            throw new BusinessException(400, "运动时长须在1-600分钟之间");
        }

        LocalDate recordDate = request.getRecordDate() != null && !request.getRecordDate().isBlank()
                ? parseDate(request.getRecordDate(), "recordDate")
                : LocalDate.now();
        validateBackfillDate(recordDate);

        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(userId);
        record.setType(request.getType());
        record.setDuration(request.getDuration());
        record.setCount(request.getCount() != null ? request.getCount() : 1);
        record.setRecordDate(recordDate);
        exerciseRecordRepository.save(record);

        return Result.success(toExerciseResponse(record), "创建成功");
    }

    @Override
    public Result<List<WeightRecordResponse>> listWeight(String userId, String startDate, String endDate) {
        validateUser(userId);
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        List<WeightRecordResponse> list = weightRecordRepository.findByUserIdAndRecordDateBetween(userId, start, end)
                .stream().map(this::toWeightResponse).toList();
        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<WeightRecordResponse> createWeight(String userId, CreateWeightRequest request) {
        validateUser(userId);
        checkAbnormalCheckin(userId, "/health/weight");

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (user.getHeight() == null) {
            throw new BusinessException(400, "请先完善身高数据以计算BMI");
        }
        if (request.getWeight() == null || request.getWeight() < 30 || request.getWeight() > 250) {
            throw new BusinessException(400, "体重须在30-250kg之间");
        }

        LocalDate recordDate = request.getRecordDate() != null && !request.getRecordDate().isBlank()
                ? parseDate(request.getRecordDate(), "recordDate")
                : LocalDate.now();
        validateBackfillDate(recordDate);

        BigDecimal weight = BigDecimal.valueOf(request.getWeight()).setScale(1, RoundingMode.HALF_UP);
        WeightRecord record = weightRecordRepository.findByUserIdAndRecordDate(userId, recordDate)
                .orElseGet(() -> {
                    WeightRecord r = new WeightRecord();
                    r.setUserId(userId);
                    r.setRecordDate(recordDate);
                    return r;
                });
        record.setWeight(weight);
        weightRecordRepository.save(record);

        user.setWeight(request.getWeight());
        userRepository.save(user);

        return Result.success(toWeightResponse(record), "记录成功");
    }

    @Override
    public Result<ChartResponse> getChart(String userId, String type, String startDate, String endDate) {
        validateUser(userId);
        if (type == null || !Set.of("sleep", "exercise", "weight").contains(type)) {
            throw new BusinessException(400, "图表类型无效");
        }

        LocalDate end = endDate != null && !endDate.isBlank() ? parseDate(endDate, "endDate") : LocalDate.now();
        LocalDate start = startDate != null && !startDate.isBlank()
                ? parseDate(startDate, "startDate")
                : end.minusDays(6);

        ChartResponse chart = new ChartResponse();
        chart.setType(type);
        chart.setStartDate(start.format(DATE_FMT));
        chart.setEndDate(end.format(DATE_FMT));

        List<String> xAxis = new ArrayList<>();
        List<Double> yAxis = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            xAxis.add(String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()));
            yAxis.add(resolveChartValue(userId, type, d));
        }

        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);
        chart.setUnit(switch (type) {
            case "sleep" -> "小时";
            case "exercise" -> "分钟";
            default -> "kg";
        });

        return Result.success(chart, "获取成功");
    }

    private Double resolveChartValue(String userId, String type, LocalDate date) {
        return switch (type) {
            case "sleep" -> sleepRecordRepository.findByUserIdAndSleepDate(userId, date)
                    .map(r -> r.getDuration().doubleValue()).orElse(null);
            case "exercise" -> {
                int total = exerciseRecordRepository.findByUserIdAndRecordDate(userId, date).stream()
                        .mapToInt(ExerciseRecord::getDuration).sum();
                yield total == 0 ? null : (double) total;
            }
            case "weight" -> weightRecordRepository.findByUserIdAndRecordDate(userId, date)
                    .map(r -> r.getWeight().doubleValue()).orElse(null);
            default -> null;
        };
    }

    private boolean hasSleepOverlap(String userId, LocalDateTime bed, LocalDateTime wake, String excludeId) {
        return sleepRecordRepository.findByUserId(userId).stream()
                .filter(r -> excludeId == null || !excludeId.equals(r.getId()))
                .anyMatch(r -> bed.isBefore(r.getWakeTime()) && r.getBedTime().isBefore(wake));
    }

    private void checkAbnormalCheckin(String userId, String endpoint) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = checkinCounters.computeIfAbsent(userId, k -> new ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > 60_000);
            timestamps.add(now);
            if (timestamps.size() > abnormalCheckinThreshold) {
                User user = userRepository.findByUserId(userId).orElse(null);
                if (user != null && !"frozen".equals(user.getStatus())) {
                    user.setStatus("frozen");
                    user.setFrozenReason("abnormal_checkin");
                    user.setFrozenTime(LocalDateTime.now().format(CREATE_TIME_FMT));
                    userRepository.save(user);

                    ExceptionLog log = new ExceptionLog();
                    log.setUserId(userId);
                    log.setExceptionType("abnormal_checkin");
                    log.setExceptionDetail("用户 " + userId + " 在1分钟内提交健康数据 "
                            + timestamps.size() + " 次，超过阈值 " + abnormalCheckinThreshold + "，已自动冻结");
                    log.setRequestUrl(endpoint);
                    exceptionLogRepository.save(log);

                    OperationLog op = new OperationLog();
                    op.setAdminId("system");
                    op.setAdminNickname("系统");
                    op.setAction("auto_freeze_user");
                    op.setActionText("系统自动冻结用户");
                    op.setTargetType("user");
                    op.setTargetId(userId);
                    op.setReasonCode("abnormal_checkin");
                    op.setReasonText("异常打卡");
                    operationLogRepository.save(op);
                }
                throw new BusinessException(403, "账号因异常打卡已被冻结", "account_frozen");
            }
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

    private void validateBackfillDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusDays(7))) {
            throw new BusinessException(400, "补录日期须在过去7天内");
        }
    }

    private SleepRecordResponse toSleepResponse(SleepRecord r) {
        SleepRecordResponse res = new SleepRecordResponse();
        res.setId(r.getId());
        res.setSleepDate(r.getSleepDate().format(DATE_FMT));
        res.setBedTime(r.getBedTime().format(DATE_TIME_FMT));
        res.setWakeTime(r.getWakeTime().format(DATE_TIME_FMT));
        res.setDuration(r.getDuration().doubleValue());
        res.setQuality(r.getQuality());
        res.setCreateTime(r.getCreateTime().format(CREATE_TIME_FMT));
        return res;
    }

    private ExerciseRecordResponse toExerciseResponse(ExerciseRecord r) {
        ExerciseRecordResponse res = new ExerciseRecordResponse();
        res.setId(r.getId());
        res.setType(r.getType());
        res.setDuration(r.getDuration());
        res.setCount(r.getCount());
        res.setRecordDate(r.getRecordDate().format(DATE_FMT));
        res.setCreateTime(r.getCreateTime().format(CREATE_TIME_FMT));
        return res;
    }

    private WeightRecordResponse toWeightResponse(WeightRecord r) {
        WeightRecordResponse res = new WeightRecordResponse();
        res.setId(r.getId());
        res.setWeight(r.getWeight().doubleValue());
        res.setRecordDate(r.getRecordDate().format(DATE_FMT));
        res.setCreateTime(r.getCreateTime().format(CREATE_TIME_FMT));
        return res;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, field + " 格式应为 YYYY-MM-DD");
        }
    }

    private LocalDateTime parseDateTimeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, field + " 不能为空");
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, field + " 格式应为 YYYY-MM-DD HH:mm");
        }
    }
}
