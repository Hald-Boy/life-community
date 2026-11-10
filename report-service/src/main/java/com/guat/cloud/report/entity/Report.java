package com.guat.cloud.report.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 举报表 report
 */
@Data
@NoArgsConstructor
@Schema(description = "举报实体（提交举报请求体）")
public class Report {

    private Long id;
    /** 举报类型：1 帖子 2 评论 */
    private Integer reportType;
    /** 被举报帖子/评论 id */
    private Long targetId;
    /** 举报人 id */
    private Long userId;
    /** 举报分类：色情/广告/侵权等 */
    private String reasonType;
    /** 补充描述 */
    private String remark;
    /** 0 待审核 1 审核通过(下架) 2 驳回 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
