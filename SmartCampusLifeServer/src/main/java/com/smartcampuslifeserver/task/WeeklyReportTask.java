package com.smartcampuslifeserver.task;

import com.smartcampuslifeserver.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReportTask {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportTask.class);

    private final ReportService reportService;

    public WeeklyReportTask(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 每周日 23:59 为所有用户生成周报。
     * cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 59 23 * * SUN")
    public void generateWeeklyReports() {
        log.info("开始执行周报定时任务...");
        try {
            reportService.generateWeeklyReportsForAllUsers();
            log.info("周报定时任务执行完成");
        } catch (Exception e) {
            log.error("周报定时任务执行失败", e);
        }
    }
}
