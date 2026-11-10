package com.guat.cloud.user.service;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.UserCardVO;

import java.util.Map;

public interface FollowService {

    /** 关注 / 取消关注，返回 {isFollowing} */
    Map<String, Object> toggleFollow(Integer userId, Integer targetUserId);

    /** 关注状态，viewerId 为空按未登录处理，返回 {isFollowing, isMutual} */
    Map<String, Object> getFollowStatus(Integer viewerId, Integer targetUserId);

    /** 查看某用户的关注列表（分页） */
    PageBean<UserCardVO> listFollowing(Integer viewerId, Integer userId, int pageNum, int pageSize);

    /** 查看某用户的粉丝列表（分页） */
    PageBean<UserCardVO> listFans(Integer viewerId, Integer userId, int pageNum, int pageSize);
}
