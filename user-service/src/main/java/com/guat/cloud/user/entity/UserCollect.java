package com.guat.cloud.user.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户收藏表 user_collect（帖子 + 评论 通用）
 */
@Data
@NoArgsConstructor
public class UserCollect {

    private Long id;
    /** 用户 id */
    private Long userId;
    /** 收藏类型：1 帖子 2 评论 */
    private Integer collectType;
    /** 帖子/评论 id */
    private Long targetId;
    /** 0 有效收藏 1 取消 */
    private Integer isCancel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
