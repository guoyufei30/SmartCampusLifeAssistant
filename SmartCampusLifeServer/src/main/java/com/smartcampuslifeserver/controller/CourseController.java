package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.CourseImportResponse;
import com.smartcampuslifeserver.dto.CourseResponse;
import com.smartcampuslifeserver.dto.CreateCourseRequest;
import com.smartcampuslifeserver.dto.CreateCourseResponse;
import com.smartcampuslifeserver.dto.UpdateCourseRequest;
import com.smartcampuslifeserver.service.CourseService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping(value = "/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        Resource resource = courseService.downloadTemplate(SecurityUtils.getUserId());
        if (!resource.exists()) {
            throw new BusinessException(404, "课表模板文件不存在");
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                // 使用 ASCII 文件名，确保 Apifox / 浏览器另存为时默认 .xlsx
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=course_template.xlsx");
        if (resource.contentLength() >= 0) {
            builder.contentLength(resource.contentLength());
        }
        return builder.body(resource);
    }

    @PostMapping("/import")
    public Result<CourseImportResponse> importCourses(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return courseService.importCourses(SecurityUtils.getUserId(), file);
    }

    @GetMapping("/list")
    public Result<List<CourseResponse>> listCourses() {
        return courseService.listCourses(SecurityUtils.getUserId());
    }

    @PostMapping
    public Result<CreateCourseResponse> createCourse(@RequestBody CreateCourseRequest request) {
        return courseService.createCourse(SecurityUtils.getUserId(), request);
    }

    @PutMapping("/{courseId}")
    public Result<Void> updateCourse(@PathVariable String courseId, @RequestBody UpdateCourseRequest request) {
        return courseService.updateCourse(SecurityUtils.getUserId(), courseId, request);
    }

    @DeleteMapping("/{courseId}")
    public Result<Void> deleteCourse(@PathVariable String courseId) {
        return courseService.deleteCourse(SecurityUtils.getUserId(), courseId);
    }
}
