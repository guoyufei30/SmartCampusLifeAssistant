package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.SemesterResponse;
import com.smartcampuslifeserver.dto.SetCurrentSemesterRequest;

import java.util.List;

public interface SemesterService {

    Result<List<SemesterResponse>> listSemesters(String userId);

    Result<Void> setCurrentSemester(String userId, SetCurrentSemesterRequest request);
}
