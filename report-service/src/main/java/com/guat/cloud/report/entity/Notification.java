package com.guat.cloud.report.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内通知表 user_notification
 */
@Data
@NoArgsConstructor
public class Notification {

    public static final int TYPE_CONTENT_REMOVED = 1; // 内容被下架（通知内容作者）
    public static final int TYPE_REPORT_REJECTED = 2; // 举报被驳回（通知举报人）

    private Long id;
    /** 接收通知的用户 id */
    private Long userId;
    /** 通知类型：1 内容被下架 2 举报被驳回 */
    private Integer type;
    private String content;
    /** 关联内容类型：1 帖子 2 评论 */
    private Integer targetType;
    /** 关联内容 id */
    private Long targetId;
    /** 0 未读 1 已读 */
    private Integer isRead;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
