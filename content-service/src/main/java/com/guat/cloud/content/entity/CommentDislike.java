package com.guat.cloud.content.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论不喜欢(折叠)表 comment_dislike
 * 语义：用户点"裂开爱心"后折叠该评论，可再次点击取消
 */
@Data
@NoArgsConstructor
public class CommentDislike {

    private Long id;
    /** 评论 id */
    private Long commentId;
    /** 用户 id */
    private Long userId;
    /** 0 有效(已折叠) 1 取消 */
    private Integer isCancel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
