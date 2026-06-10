package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.report.RegenerateReportRequest;
import com.smartcampuslifeserver.dto.report.WeeklyReportHistoryItem;
import com.smartcampuslifeserver.dto.report.WeeklyReportResponse;

import java.util.List;

public interface ReportService {

    Result<WeeklyReportResponse> getCurrentWeeklyReport(String userId);

    Result<List<WeeklyReportHistoryItem>> getHistory(String userId);

    Result<WeeklyReportResponse> regenerate(String userId, RegenerateReportRequest request);

    Result<WeeklyReportResponse> getDetail(String userId, Long reportId);

    void generateWeeklyReportsForAllUsers();
}
