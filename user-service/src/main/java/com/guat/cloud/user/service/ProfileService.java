package com.guat.cloud.user.service;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.PostCardVO;
import com.guat.cloud.user.dto.UserProfileVO;

public interface ProfileService {

    /** 个人主页：用户信息 + 作品数 + 关注/互关状态 */
    UserProfileVO getProfile(Integer viewerId, Integer userId);

    /** 某用户的作品列表（分页） */
    PageBean<PostCardVO> listUserPosts(Integer userId, int pageNum, int pageSize);
}
