package com.guat.cloud.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人主页数据
 */
@Data
@NoArgsConstructor
public class UserProfileVO {

    /** 用户基本信息（含头像/昵称/简介/获赞/关注数/粉丝数等） */
    private UserInfo userInfo;
    /** 作品数 */
    private Integer postCount;
    /** 当前登录用户是否已关注该用户 */
    private Boolean isFollowing;
    /** 是否互关 */
    private Boolean isMutual;
}
