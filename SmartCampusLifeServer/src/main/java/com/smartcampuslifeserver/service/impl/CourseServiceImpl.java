package com.smartcampuslifeserver.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.CourseImportDTO;
import com.smartcampuslifeserver.dto.CourseImportResponse;
import com.smartcampuslifeserver.dto.CourseResponse;
import com.smartcampuslifeserver.dto.CreateCourseRequest;
import com.smartcampuslifeserver.dto.CreateCourseResponse;
import com.smartcampuslifeserver.dto.UpdateCourseRequest;
import com.smartcampuslifeserver.entity.Course;
import com.smartcampuslifeserver.entity.ExceptionLog;
import com.smartcampuslifeserver.entity.ScheduleEvent;
import com.smartcampuslifeserver.entity.Semester;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.repository.CourseRepository;
import com.smartcampuslifeserver.repository.ExceptionLogRepository;
import com.smartcampuslifeserver.repository.ScheduleEventRepository;
import com.smartcampuslifeserver.repository.SemesterRepository;
import com.smartcampuslifeserver.repository.UserRepository;
import com.smartcampuslifeserver.service.CourseService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourseServiceImpl implements CourseService {

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final int MIN_PERIOD = 1;
    private static final int MAX_PERIOD = 10;
    private static final Pattern WEEK_SEGMENT = Pattern.compile("^(\\d+)-(\\d+)周(单周|双周)?$");
    private static final List<String> SENSITIVE_WORDS = List.of(
            "违禁", "管理员", "暴力", "色情", "赌博", "fuck", "admin", "傻逼", "毒品"
    );

    private static final LocalTime[] PERIOD_STARTS = {
            null,
            LocalTime.of(8, 0),
            LocalTime.of(8, 55),
            LocalTime.of(10, 0),
            LocalTime.of(10, 55),
            LocalTime.of(14, 0),
            LocalTime.of(14, 55),
            LocalTime.of(16, 0),
            LocalTime.of(16, 55),
            LocalTime.of(19, 0),
            LocalTime.of(19, 55)
    };

    private static final LocalTime[] PERIOD_ENDS = {
            null,
            LocalTime.of(8, 45),
            LocalTime.of(9, 40),
            LocalTime.of(10, 45),
            LocalTime.of(11, 40),
            LocalTime.of(14, 45),
            LocalTime.of(15, 40),
            LocalTime.of(16, 45),
            LocalTime.of(17, 40),
            LocalTime.of(19, 45),
            LocalTime.of(20, 40)
    };

    private final CourseRepository courseRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final ExceptionLogRepository exceptionLogRepository;

    public CourseServiceImpl(CourseRepository courseRepository,
                             ScheduleEventRepository scheduleEventRepository,
                             SemesterRepository semesterRepository,
                             UserRepository userRepository,
                             ExceptionLogRepository exceptionLogRepository) {
        this.courseRepository = courseRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.exceptionLogRepository = exceptionLogRepository;
    }

    @Override
    public Resource downloadTemplate(String userId) {
        validateUser(userId);
        return new ClassPathResource("templates/course_template.xlsx");
    }

    @Override
    @Transactional
    public Result<CourseImportResponse> importCourses(String userId, MultipartFile file) {
        validateUser(userId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传 Excel 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过 2MB");
        }
        if (!isExcelFile(file)) {
            throw new BusinessException(400, "仅支持 Excel 文件（.xlsx/.xls）");
        }

        Long semesterId = resolveCurrentSemesterId();
        List<ParsedRow> rows = parseExcel(file);
        if (rows.isEmpty()) {
            throw new BusinessException(400, "导入失败：文件中没有有效数据");
        }

        List<Course> parsedCourses = new ArrayList<>();
        for (ParsedRow row : rows) {
            parsedCourses.add(validateAndBuildCourse(userId, semesterId, row));
        }

        List<Course> existingCourses = courseRepository.findByUserIdAndSemesterId(userId, semesterId);
        checkConflicts(rows, parsedCourses, existingCourses);

        LocalDate referenceDate = resolveReferenceDate(semesterId);
        for (Course course : parsedCourses) {
            saveCourseWithEvent(userId, course, referenceDate);
        }

        CourseImportResponse response = new CourseImportResponse();
        response.setSuccessCount(parsedCourses.size());
        response.setFailCount(0);
        return Result.success(response, "导入成功，共导入" + parsedCourses.size() + "门课程");
    }

    @Override
    public Result<List<CourseResponse>> listCourses(String userId) {
        validateUser(userId);
        Long semesterId = resolveCurrentSemesterId();
        List<CourseResponse> list = courseRepository.findByUserIdAndSemesterId(userId, semesterId)
                .stream()
                .sorted(Comparator.comparing(Course::getDayOfWeek).thenComparing(Course::getStartPeriod))
                .map(this::toCourseResponse)
                .toList();
        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<CreateCourseResponse> createCourse(String userId, CreateCourseRequest request) {
        validateUser(userId);
        Long semesterId = resolveCurrentSemesterId();
        Course course = validateAndBuildCourse(userId, semesterId, request);

        List<Course> existingCourses = courseRepository.findByUserIdAndSemesterId(userId, semesterId);
        for (Course existing : existingCourses) {
            if (hasTimeConflict(course, existing)) {
                throw new BusinessException(409,
                        "添加失败：与已有课程[" + existing.getName() + "]发生时间冲突，请修改时间。");
            }
        }

        LocalDate referenceDate = resolveReferenceDate(semesterId);
        ScheduleEvent event = saveCourseWithEvent(userId, course, referenceDate);

        CreateCourseResponse response = new CreateCourseResponse();
        response.setCourseId(course.getId());
        response.setEventId(event.getId());
        return Result.success(response, "添加成功");
    }

    @Override
    @Transactional
    public Result<Void> updateCourse(String userId, String courseId, UpdateCourseRequest request) {
        validateUser(userId);
        Course existing = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException(404, "课程不存在"));

        CourseFields fields = validateCourseFields(userId, "/course/" + courseId, request.getTitle(),
                request.getWeekPattern(), request.getDayOfWeek(), request.getStartPeriod(),
                request.getEndPeriod(), request.getLocation());

        Course candidate = buildCourse(userId, existing.getSemesterId(), fields.name(), fields.weekPattern(),
                fields.dayOfWeek(), fields.startPeriod(), fields.endPeriod(), fields.location());
        candidate.setId(courseId);

        List<Course> semesterCourses = courseRepository.findByUserIdAndSemesterId(userId, existing.getSemesterId());
        for (Course other : semesterCourses) {
            if (other.getId().equals(courseId)) {
                continue;
            }
            if (hasTimeConflict(candidate, other)) {
                throw new BusinessException(409,
                        "修改失败：与已有课程[" + other.getName() + "]发生时间冲突，请修改时间。");
            }
        }

        existing.setName(fields.name());
        existing.setWeekPattern(fields.weekPattern());
        existing.setDayOfWeek(fields.dayOfWeek());
        existing.setStartPeriod(fields.startPeriod());
        existing.setEndPeriod(fields.endPeriod());
        existing.setLocation(fields.location());
        courseRepository.save(existing);

        LocalDate referenceDate = resolveReferenceDate(existing.getSemesterId());
        ScheduleEvent event = scheduleEventRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException(404, "关联日程不存在"));
        event.setTitle(existing.getName());
        event.setLocation(existing.getLocation());
        event.setStartTime(buildStartDateTime(referenceDate, existing.getDayOfWeek(), existing.getStartPeriod()));
        event.setEndTime(buildEndDateTime(referenceDate, existing.getDayOfWeek(), existing.getEndPeriod()));
        scheduleEventRepository.save(event);

        return Result.success(null, "更新成功");
    }

    @Override
    @Transactional
    public Result<Void> deleteCourse(String userId, String courseId) {
        validateUser(userId);
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException(404, "课程不存在"));

        scheduleEventRepository.findByCourseIdAndUserId(courseId, userId)
                .ifPresent(scheduleEventRepository::delete);
        courseRepository.delete(course);

        return Result.success(null, "删除成功");
    }

    private ScheduleEvent saveCourseWithEvent(String userId, Course course, LocalDate referenceDate) {
        courseRepository.save(course);

        LocalDateTime start = buildStartDateTime(referenceDate, course.getDayOfWeek(), course.getStartPeriod());
        LocalDateTime end = buildEndDateTime(referenceDate, course.getDayOfWeek(), course.getEndPeriod());

        ScheduleEvent event = new ScheduleEvent();
        event.setUserId(userId);
        event.setTitle(course.getName());
        event.setCategory("course");
        event.setStartTime(start);
        event.setEndTime(end);
        event.setLocation(course.getLocation());
        event.setReminder("15min");
        event.setStatus("pending");
        event.setCourseId(course.getId());
        event.setReminderAcked(false);
        return scheduleEventRepository.save(event);
    }

    private Course validateAndBuildCourse(String userId, Long semesterId, CreateCourseRequest request) {
        CourseFields fields = validateCourseFields(userId, "/course", request.getTitle(),
                request.getWeekPattern(), request.getDayOfWeek(), request.getStartPeriod(),
                request.getEndPeriod(), request.getLocation());
        return buildCourse(userId, semesterId, fields.name(), fields.weekPattern(),
                fields.dayOfWeek(), fields.startPeriod(), fields.endPeriod(), fields.location());
    }

    private CourseFields validateCourseFields(String userId, String requestUrl, String title,
                                              String weekPattern, Integer dayOfWeek,
                                              Integer startPeriod, Integer endPeriod, String location) {
        String name = trimToNull(title);
        if (name == null) {
            throw new BusinessException(400, "课程名称不能为空");
        }
        if (name.length() > 20) {
            throw new BusinessException(400, "课程名称不能超过20个字符");
        }
        checkSensitiveName(userId, name, requestUrl);

        String pattern = trimToNull(weekPattern);
        if (pattern == null || !isValidWeekPattern(pattern)) {
            throw new BusinessException(400, "周次格式无法识别");
        }

        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            throw new BusinessException(400, "星期须在1-7之间");
        }

        if (startPeriod == null || endPeriod == null) {
            throw new BusinessException(400, "节次不能为空");
        }
        if (startPeriod < MIN_PERIOD || startPeriod > MAX_PERIOD
                || endPeriod < MIN_PERIOD || endPeriod > MAX_PERIOD) {
            throw new BusinessException(400, "节次须在1-10之间");
        }
        if (startPeriod > endPeriod) {
            throw new BusinessException(400, "起始节次不能大于结束节次");
        }

        String loc = trimToNull(location);
        if (loc != null && loc.length() > 50) {
            throw new BusinessException(400, "上课地点不能超过50个字符");
        }

        return new CourseFields(name, pattern, dayOfWeek, startPeriod, endPeriod, loc);
    }

    private Course buildCourse(String userId, Long semesterId, String name, String weekPattern,
                               int dayOfWeek, int startPeriod, int endPeriod, String location) {
        Course course = new Course();
        course.setUserId(userId);
        course.setSemesterId(semesterId);
        course.setName(name);
        course.setWeekPattern(weekPattern);
        course.setDayOfWeek(dayOfWeek);
        course.setStartPeriod(startPeriod);
        course.setEndPeriod(endPeriod);
        course.setLocation(location);
        return course;
    }

    private CourseResponse toCourseResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setTitle(course.getName());
        response.setWeekPattern(course.getWeekPattern());
        response.setDayOfWeek(course.getDayOfWeek());
        response.setStartPeriod(course.getStartPeriod());
        response.setEndPeriod(course.getEndPeriod());
        response.setLocation(course.getLocation());
        return response;
    }

    private List<ParsedRow> parseExcel(MultipartFile file) {
        List<ParsedRow> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, CourseImportDTO.class, new AnalysisEventListener<CourseImportDTO>() {
                @Override
                public void invoke(CourseImportDTO data, AnalysisContext context) {
                    if (isEmptyRow(data)) {
                        return;
                    }
                    int rowNum = context.readRowHolder().getRowIndex() + 1;
                    rows.add(new ParsedRow(rowNum, data));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException(400, "导入失败：无法读取 Excel 文件");
        } catch (Exception e) {
            throw new BusinessException(400, "导入失败：Excel 格式不正确");
        }
        return rows;
    }

    private Course validateAndBuildCourse(String userId, Long semesterId, ParsedRow row) {
        CourseImportDTO data = row.data();
        int rowNum = row.rowNum();

        String name = trimToNull(data.getName());
        if (name == null) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行课程名称不能为空");
        }
        if (name.length() > 20) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行课程名称不能超过20个字符");
        }
        checkSensitiveName(userId, name, rowNum);

        String weekPattern = trimToNull(data.getWeekPattern());
        if (weekPattern == null || !isValidWeekPattern(weekPattern)) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行周次格式无法识别");
        }

        Integer dayOfWeek = parsePositiveInt(data.getDayOfWeek(), rowNum, "星期");
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行星期须在1-7之间");
        }

        Integer startPeriod = parsePositiveInt(data.getStartPeriod(), rowNum, "起始节次");
        Integer endPeriod = parsePositiveInt(data.getEndPeriod(), rowNum, "结束节次");
        if (startPeriod < MIN_PERIOD || startPeriod > MAX_PERIOD
                || endPeriod < MIN_PERIOD || endPeriod > MAX_PERIOD) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行节次须在1-10之间");
        }
        if (startPeriod > endPeriod) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行起始节次不能大于结束节次");
        }

        String location = trimToNull(data.getLocation());
        if (location != null && location.length() > 50) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行上课地点不能超过50个字符");
        }

        return buildCourse(userId, semesterId, name, weekPattern, dayOfWeek, startPeriod, endPeriod, location);
    }

    private void checkConflicts(List<ParsedRow> rows, List<Course> parsedCourses, List<Course> existingCourses) {
        for (int i = 0; i < parsedCourses.size(); i++) {
            Course current = parsedCourses.get(i);
            int rowNum = rows.get(i).rowNum();

            for (Course existing : existingCourses) {
                if (hasTimeConflict(current, existing)) {
                    throw new BusinessException(409,
                            "导入失败：第" + rowNum + "行与已有课程[" + existing.getName() + "]发生时间冲突");
                }
            }

            for (int j = 0; j < i; j++) {
                Course previous = parsedCourses.get(j);
                if (hasTimeConflict(current, previous)) {
                    throw new BusinessException(409,
                            "导入失败：第" + rowNum + "行与第" + rows.get(j).rowNum() + "行课程["
                                    + previous.getName() + "]发生时间冲突");
                }
            }
        }
    }

    private boolean hasTimeConflict(Course a, Course b) {
        if (!a.getDayOfWeek().equals(b.getDayOfWeek())) {
            return false;
        }
        if (!isPeriodOverlap(a.getStartPeriod(), a.getEndPeriod(),
                b.getStartPeriod(), b.getEndPeriod())) {
            return false;
        }
        Set<Integer> weeksA = parseWeeks(a.getWeekPattern());
        Set<Integer> weeksB = parseWeeks(b.getWeekPattern());
        for (Integer week : weeksA) {
            if (weeksB.contains(week)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPeriodOverlap(int start1, int end1, int start2, int end2) {
        return start1 <= end2 && start2 <= end1;
    }

    private boolean isValidWeekPattern(String pattern) {
        try {
            parseWeeks(pattern);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private Set<Integer> parseWeeks(String pattern) {
        Set<Integer> weeks = new HashSet<>();
        String[] segments = pattern.split("[,，]");
        for (String segment : segments) {
            String part = segment.trim();
            if (part.isEmpty()) {
                throw new BusinessException(400, "周次格式无法识别");
            }
            Matcher matcher = WEEK_SEGMENT.matcher(part);
            if (!matcher.matches()) {
                throw new BusinessException(400, "周次格式无法识别");
            }
            int start = Integer.parseInt(matcher.group(1));
            int end = Integer.parseInt(matcher.group(2));
            if (start < 1 || end < start || end > 30) {
                throw new BusinessException(400, "周次范围无效");
            }
            String weekType = matcher.group(3);
            for (int week = start; week <= end; week++) {
                if (weekType == null) {
                    weeks.add(week);
                } else if ("单周".equals(weekType) && week % 2 == 1) {
                    weeks.add(week);
                } else if ("双周".equals(weekType) && week % 2 == 0) {
                    weeks.add(week);
                }
            }
        }
        if (weeks.isEmpty()) {
            throw new BusinessException(400, "周次格式无法识别");
        }
        return weeks;
    }

    private void checkSensitiveName(String userId, String name, int rowNum) {
        String word = wordInName(name);
        if (word == null) {
            return;
        }
        saveViolationLog(userId, "课程名称包含违禁词：" + word + "（第" + rowNum + "行）", "/course/import");
        throw new BusinessException(400, "导入失败：第" + rowNum + "行课程名称包含违禁词汇");
    }

    private void checkSensitiveName(String userId, String name, String requestUrl) {
        String word = wordInName(name);
        if (word == null) {
            return;
        }
        saveViolationLog(userId, "课程名称包含违禁词：" + word, requestUrl);
        throw new BusinessException(400, "内容包含违禁词汇，请修改后重试");
    }

    private void saveViolationLog(String userId, String detail, String requestUrl) {
        ExceptionLog log = new ExceptionLog();
        log.setUserId(userId);
        log.setExceptionType("content_violation");
        log.setExceptionDetail(detail);
        log.setRequestUrl(requestUrl);
        exceptionLogRepository.save(log);
    }

    private String wordInName(String name) {
        String lower = name.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                return word;
            }
        }
        return null;
    }

    private LocalDate resolveReferenceDate(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId).orElse(null);
        LocalDate base = semester != null ? semester.getStartDate() : LocalDate.now();
        if (base.isBefore(LocalDate.now().minusMonths(6))) {
            base = LocalDate.now();
        }
        return base;
    }

    private LocalDateTime buildStartDateTime(LocalDate referenceDate, int dayOfWeek, int startPeriod) {
        LocalDate date = alignToDayOfWeek(referenceDate, dayOfWeek);
        return date.atTime(PERIOD_STARTS[startPeriod]);
    }

    private LocalDateTime buildEndDateTime(LocalDate referenceDate, int dayOfWeek, int endPeriod) {
        LocalDate date = alignToDayOfWeek(referenceDate, dayOfWeek);
        return date.atTime(PERIOD_ENDS[endPeriod]);
    }

    private LocalDate alignToDayOfWeek(LocalDate referenceDate, int dayOfWeek) {
        LocalDate date = referenceDate;
        while (date.getDayOfWeek().getValue() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private Long resolveCurrentSemesterId() {
        return semesterRepository.findByIsCurrentTrue()
                .map(Semester::getId)
                .orElse(1L);
    }

    private Integer parsePositiveInt(String value, int rowNum, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行" + field + "不能为空");
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "导入失败：第" + rowNum + "行" + field + "格式无效");
        }
    }

    private boolean isEmptyRow(CourseImportDTO data) {
        return trimToNull(data.getName()) == null
                && trimToNull(data.getWeekPattern()) == null
                && trimToNull(data.getDayOfWeek()) == null
                && trimToNull(data.getStartPeriod()) == null
                && trimToNull(data.getEndPeriod()) == null
                && trimToNull(data.getLocation()) == null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return true;
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String type = contentType.toLowerCase();
            if (type.contains("spreadsheetml") || type.contains("ms-excel")) {
                return true;
            }
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(4);
            // xlsx 本质是 ZIP，文件头为 PK
            return header.length >= 2 && header[0] == 'P' && header[1] == 'K';
        } catch (IOException e) {
            return false;
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

    private record ParsedRow(int rowNum, CourseImportDTO data) {
    }

    private record CourseFields(String name, String weekPattern, int dayOfWeek,
                                int startPeriod, int endPeriod, String location) {
    }
}
