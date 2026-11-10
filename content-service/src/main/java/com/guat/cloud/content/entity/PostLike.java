package com.guat.cloud.content.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子点赞表 post_like
 */
@Data
@NoArgsConstructor
public class PostLike {

    private Long id;
    /** 帖子 id */
    private Long newsId;
    /** 点赞用户 id */
    private Long userId;
    /** 0 有效点赞 1 取消点赞 */
    private Integer isCancel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
