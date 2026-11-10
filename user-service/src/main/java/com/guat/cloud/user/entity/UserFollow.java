package com.guat.cloud.user.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户关注表 user_follow
 */
@Data
@NoArgsConstructor
public class UserFollow {

    private Long id;
    /** 关注者 id */
    private Long userId;
    /** 被关注人 id */
    private Long followUserId;
    /** 0 有效关注 1 取消 */
    private Integer isCancel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
