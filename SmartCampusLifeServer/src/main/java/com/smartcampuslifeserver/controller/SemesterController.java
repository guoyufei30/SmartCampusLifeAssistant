package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.SemesterResponse;
import com.smartcampuslifeserver.dto.SetCurrentSemesterRequest;
import com.smartcampuslifeserver.service.SemesterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/semester")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping("/list")
    public Result<List<SemesterResponse>> listSemesters() {
        return semesterService.listSemesters(SecurityUtils.getUserId());
    }

    @PutMapping("/current")
    public Result<Void> setCurrentSemester(@RequestBody SetCurrentSemesterRequest request) {
        return semesterService.setCurrentSemester(SecurityUtils.getUserId(), request);
    }
}
