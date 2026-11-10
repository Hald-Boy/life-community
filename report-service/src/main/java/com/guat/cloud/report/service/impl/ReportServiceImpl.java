package com.guat.cloud.report.service.impl;

import com.guat.cloud.report.entity.Report;
import com.guat.cloud.report.exception.BusinessException;
import com.guat.cloud.report.mapper.CommentMapper;
import com.guat.cloud.report.mapper.NewsMapper;
import com.guat.cloud.report.mapper.ReportMapper;
import com.guat.cloud.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public void submitReport(Integer userId, Report report) {
        if (report.getReportType() == null || (report.getReportType() != 1 && report.getReportType() != 2)) {
            throw new BusinessException("举报类型错误");
        }
        if (report.getReportType() == 1) {
            if (newsMapper.findById(report.getTargetId().intValue()) == null) {
                throw new BusinessException("帖子不存在");
            }
        } else {
            if (commentMapper.selectById(report.getTargetId()) == null) {
                throw new BusinessException("评论不存在");
            }
        }
        if (report.getReasonType() == null || report.getReasonType().trim().isEmpty()) {
            throw new BusinessException("请选择举报原因");
        }
        // 同一用户对同一目标已有待审核举报 -> 防重复
        Report exist = reportMapper.findByUserAndTarget(userId.longValue(), report.getReportType(), report.getTargetId());
        if (exist != null && Integer.valueOf(0).equals(exist.getStatus())
                && (exist.getIsDeleted() == null || exist.getIsDeleted() == 0)) {
            throw new BusinessException("已收到你的反馈，请勿重复举报");
        }

        report.setUserId(userId.longValue());
        report.setStatus(0);

        reportMapper.insert(report);
    }
}
