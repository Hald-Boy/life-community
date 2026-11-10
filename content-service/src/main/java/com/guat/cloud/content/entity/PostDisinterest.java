package com.guat.cloud.content.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子不感兴趣表 user_dislike_post
 */
@Data
@NoArgsConstructor
public class PostDisinterest {

    private Long id;
    /** 帖子 id */
    private Long postId;
    /** 用户 id */
    private Long userId;
    /** 0 有效(不感兴趣) 1 取消 */
    private Integer isCancel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
