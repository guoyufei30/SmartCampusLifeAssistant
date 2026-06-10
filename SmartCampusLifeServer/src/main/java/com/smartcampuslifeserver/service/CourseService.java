package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.CourseImportResponse;
import com.smartcampuslifeserver.dto.CourseResponse;
import com.smartcampuslifeserver.dto.CreateCourseRequest;
import com.smartcampuslifeserver.dto.CreateCourseResponse;
import com.smartcampuslifeserver.dto.UpdateCourseRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService {

    Resource downloadTemplate(String userId);

    Result<CourseImportResponse> importCourses(String userId, MultipartFile file);

    Result<List<CourseResponse>> listCourses(String userId);

    Result<CreateCourseResponse> createCourse(String userId, CreateCourseRequest request);

    Result<Void> updateCourse(String userId, String courseId, UpdateCourseRequest request);

    Result<Void> deleteCourse(String userId, String courseId);
}
