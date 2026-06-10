package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.SemesterResponse;
import com.smartcampuslifeserver.dto.SetCurrentSemesterRequest;
import com.smartcampuslifeserver.entity.Semester;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.repository.SemesterRepository;
import com.smartcampuslifeserver.repository.UserRepository;
import com.smartcampuslifeserver.service.SemesterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class SemesterServiceImpl implements SemesterService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    public SemesterServiceImpl(SemesterRepository semesterRepository, UserRepository userRepository) {
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Result<List<SemesterResponse>> listSemesters(String userId) {
        validateUser(userId);
        List<SemesterResponse> list = semesterRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Semester::getStartDate).reversed())
                .map(this::toResponse)
                .toList();
        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<Void> setCurrentSemester(String userId, SetCurrentSemesterRequest request) {
        validateUser(userId);

        if (request.getSemesterId() == null) {
            throw new BusinessException(400, "学期ID不能为空");
        }

        Semester target = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new BusinessException(404, "学期不存在"));

        for (Semester semester : semesterRepository.findByIsCurrent(true)) {
            semester.setIsCurrent(false);
            semesterRepository.save(semester);
        }

        target.setIsCurrent(true);
        semesterRepository.save(target);

        return Result.success(null, "设置成功");
    }

    private SemesterResponse toResponse(Semester semester) {
        SemesterResponse response = new SemesterResponse();
        response.setId(semester.getId());
        response.setName(semester.getName());
        response.setStartDate(semester.getStartDate().format(DATE_FMT));
        response.setEndDate(semester.getEndDate().format(DATE_FMT));
        return response;
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
