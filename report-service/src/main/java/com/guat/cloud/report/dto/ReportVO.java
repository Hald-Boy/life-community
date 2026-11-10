package com.guat.cloud.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端-举报列表项
 */
@Data
@NoArgsConstructor
public class ReportVO {

    private Long reportId;
    /** 举报类型：1 帖子 2 评论 */
    private Integer reportType;
    private Long targetId;
    /** 举报分类：色情/广告/侵权等 */
    private String reasonType;
    private String remark;
    /** 0 待审核 1 审核通过(下架) 2 驳回 */
    private Integer status;
    private LocalDateTime createTime;
    /** 被举报内容标题（帖子标题 / 评论所属帖子标题） */
    private String targetTitle;
    /** 被举报内容（帖子前200字 / 评论全文） */
    private String targetContent;
    /** 举报人昵称 */
    private String reporterName;
    /** 被举报内容作者昵称 */
    private String targetUserName;
}
