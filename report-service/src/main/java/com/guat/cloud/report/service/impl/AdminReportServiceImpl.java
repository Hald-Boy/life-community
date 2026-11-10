package com.guat.cloud.report.service.impl;

import com.guat.cloud.report.dto.PageBean;
import com.guat.cloud.report.dto.ReportHandleResult;
import com.guat.cloud.report.dto.ReportVO;
import com.guat.cloud.report.entity.Comment;
import com.guat.cloud.report.entity.News;
import com.guat.cloud.report.entity.Notification;
import com.guat.cloud.report.entity.Report;
import com.guat.cloud.report.exception.BusinessException;
import com.guat.cloud.report.mapper.CommentMapper;
import com.guat.cloud.report.mapper.NewsMapper;
import com.guat.cloud.report.mapper.ReportMapper;
import com.guat.cloud.report.service.AdminReportService;
import com.guat.cloud.report.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminReportServiceImpl implements AdminReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private NotificationService notificationService;



    /**
     * 举报列表（管理端），status 可空=全部：0待审核 1已下架 2已驳回
     * 作者:豆包/中哥
     * 日期:2026/9/14
     * @param status 筛选待审核/已下架/已驳回的举报记录,status为空则查询全部
     * @param pageNum 页码
     * @param pageSize 一页记录数
     * @return 返回举报列表
     */
    @Override
    public PageBean<ReportVO> listReports(Integer status, int pageNum, int pageSize) {
        // 查询举报记录数
        long total = reportMapper.countReports(status);
        // 获取列表
        List<ReportVO> list = reportMapper.listReports(status, (pageNum - 1) * pageSize, pageSize);
        return new PageBean<>(list, total, pageNum, pageSize);
    }

    /**
     * 审核举报：action 1通过(下架目标) 2驳回
     * 作者:豆包/中哥
     * 日期:2026/9/14
     * @param adminId 管理员id
     * @param reportId 举报记录id
     * @param action 1通过(下架目标) 2驳回
     * @param remark 备注
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportHandleResult handleReport(Integer adminId, Long reportId, Integer action, String remark) {
        // 根据举报记录id查询是否存在举报记录
        Report report = reportMapper.findById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        // 如果已存在举报记录，且状态不为0（待审核），说明举报已经处理
        if (!Integer.valueOf(0).equals(report.getStatus())) {
            throw new BusinessException("我们已收到并处理您的举报，举报结果可在消息大厅查看");
        }

        // 封装返回结果：举报记录id、举报的类型（1帖子，2评论）、举报目标id、管理员处理行为
        ReportHandleResult result = new ReportHandleResult();
        result.setReportId(reportId);
        result.setReportType(report.getReportType());
        result.setTargetId(report.getTargetId());
        result.setAction(action);
        // 举报记录存在状态为0待审核，管理员行为选择1下架
        if (Integer.valueOf(1).equals(action)) {
            // 审核通过：把举报记录的状态设为1 已下架处理
            reportMapper.updateStatus(reportId, 1);
            // 继续封装结果，设置【举报记录】最新的状态
            result.setNewStatus(1);
            // 如果举报类型是帖子
            if (Integer.valueOf(1).equals(report.getReportType())) {
                // 帖子下架：news.status = 1：删除，0：正常
                newsMapper.updateStatus(report.getTargetId().intValue(), 1);
                // 继续封装结果，设置【帖子】的状态为1 下架
                result.setTargetStatus(1);
                result.setMessage("举报已通过，帖子已下架");
                // 通知内容作者
                // 获取被举报的那条帖子
                News news = newsMapper.findById(report.getTargetId().intValue());
                // 传入【帖子作者id】、  【1内容被下架】、 【内容】、   【目标类型1帖子，2评论】、  【目标内容的id】
                notificationService.notifyUser(news.getUserId().longValue(), Notification.TYPE_CONTENT_REMOVED,
                        "你的帖子《" + news.getTitle() + "》因违反社区规定已被下架",
                        1, report.getTargetId());
            } else {
                // 评论删除：comment.status = 0：删除，1正常
                commentMapper.updateStatus(report.getTargetId(), 0);
                // 继续封装结果，设置【评论】的状态为0 下架
                result.setTargetStatus(0);
                result.setMessage("举报已通过，评论已删除");
                // 通知内容作者
                Comment comment = commentMapper.selectById(report.getTargetId());
                // 传入【评论作者id】、  【1内容被下架】、 【内容】、   【目标类型1帖子，2评论】、  【目标内容的id】
                notificationService.notifyUser(comment.getUserId().longValue(), Notification.TYPE_CONTENT_REMOVED,
                        "你的评论《" + comment.getContent() + "》因违反社区规定已被删除",
                        2, report.getTargetId());
            }
        } else if (Integer.valueOf(2).equals(action)) {
            // 驳回：仅标记，目标内容不动，通知举报人
            // 0待审核，1下架，2驳回
            reportMapper.updateStatus(reportId, 2);
            // 继续封装结果，设置【举报记录】最新的状态
            result.setNewStatus(2);
            result.setMessage("举报已驳回，内容未处理");
            // 传入【举报者id】、  【2举报被驳回】、 【内容】、   【举报的类型1帖子 2评论】、  【目标内容的id】
            notificationService.notifyUser(report.getUserId(), Notification.TYPE_REPORT_REJECTED,
                    "我们还在持续对您举报的对象进行核查分析中，会进一步收集并审核更大范围的数据与用户行为，请关注后续安全邮件通知来获取结果反馈。您的举报将帮助我们持续优化对这类违规行为的检测方案，感谢您的举报，祝您生活愉快【G.T.I.Security】",
                    report.getReportType(), report.getTargetId());
        } else {
            throw new BusinessException("审核操作类型错误（1通过下架 2驳回）");
        }
        return result;
    }
}
