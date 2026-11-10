package com.guat.cloud.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端-举报审核结果
 */
@Data
@NoArgsConstructor
public class ReportHandleResult {

    private Long reportId;
    /** 举报类型：1 帖子 2 评论 */
    private Integer reportType;
    private Long targetId;
    /** 本次操作：1 通过(下架) 2 驳回 */
    private Integer action;
    /** 处理后举报状态：1 已下架 2 已驳回 */
    private Integer newStatus;
    /** 目标内容处理后的状态：帖子 1=下架；评论 0=删除；驳回(内容未动)时为 null */
    private Integer targetStatus;
    /** 结果描述：如"举报已通过，帖子已下架" */
    private String message;
}
