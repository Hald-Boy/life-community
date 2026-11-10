package com.guat.cloud.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注/粉丝列表项
 */
@Data
@NoArgsConstructor
public class UserCardVO {

    private UserInfo userInfo;
    /** 当前登录用户是否已关注该用户 */
    private Boolean isFollowing;
    /** 是否互关 */
    private Boolean isMutual;
}
