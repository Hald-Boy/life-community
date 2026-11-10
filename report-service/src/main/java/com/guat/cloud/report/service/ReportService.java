package com.guat.cloud.report.service;

import com.guat.cloud.report.entity.Report;

public interface ReportService {

    /** 提交举报，reportType 1帖子 2评论 */
    void submitReport(Integer userId, Report report);
}
