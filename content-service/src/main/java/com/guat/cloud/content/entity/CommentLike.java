package com.guat.cloud.content.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论点赞表 comment_like
 */
@Data
@NoArgsConstructor
public class CommentLike {

    private Long id;
    /** 评论 id */
    private Long commentId;
    /** 点赞用户 id */
    private Long userId;
    /** 0 有效点赞 1 取消点赞 */
    private Integer isCancel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
