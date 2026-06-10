package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.report.RegenerateReportRequest;
import com.smartcampuslifeserver.dto.report.WeeklyReportHistoryItem;
import com.smartcampuslifeserver.dto.report.WeeklyReportResponse;
import com.smartcampuslifeserver.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/report/weekly")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Result<WeeklyReportResponse> getCurrentReport() {
        return reportService.getCurrentWeeklyReport(SecurityUtils.getUserId());
    }

    @GetMapping("/history")
    public Result<List<WeeklyReportHistoryItem>> getHistory() {
        return reportService.getHistory(SecurityUtils.getUserId());
    }

    @PostMapping("/regenerate")
    public Result<WeeklyReportResponse> regenerate(
            @RequestBody(required = false) RegenerateReportRequest request) {
        return reportService.regenerate(SecurityUtils.getUserId(), request);
    }

    @GetMapping("/{reportId}")
    public Result<WeeklyReportResponse> getDetail(@PathVariable Long reportId) {
        return reportService.getDetail(SecurityUtils.getUserId(), reportId);
    }
}
