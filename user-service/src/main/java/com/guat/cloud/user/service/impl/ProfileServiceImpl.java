package com.guat.cloud.user.service.impl;

import com.guat.cloud.user.dto.*;
import com.guat.cloud.user.entity.User;
import com.guat.cloud.user.exception.BusinessException;
import com.guat.cloud.user.mapper.NewsMapper;
import com.guat.cloud.user.mapper.UserMapper;
import com.guat.cloud.user.service.FollowService;
import com.guat.cloud.user.service.ProfileService;
import com.guat.cloud.user.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private FollowService followService;

    /**
     * 个人主页：用户信息 + 作品数 + 关注/互关状态
     *
     * @param viewerId 谁看主页
     * @param userId 被看的主页是谁的
     * @return 返回用户个人主页的卡片
     */
    @Override
    public UserProfileVO getProfile(Integer viewerId, Integer userId) {
        //用户个人信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        //获取作品数
        int postCount = newsMapper.countByUserId(userId);
        //关注状态，用于前端决定渲染“已关注”还是“相互关注”
        Map<String, Object> status = followService.getFollowStatus(viewerId, userId);

        UserProfileVO vo = new UserProfileVO();
        // 封装个人信息
        vo.setUserInfo(UserInfo.from(user));
        // 封装作品数
        vo.setPostCount(postCount);
        // 设置关注状态
        vo.setIsFollowing((Boolean) status.get("isFollowing"));
        vo.setIsMutual((Boolean) status.get("isMutual"));
        return vo;
    }

    /**
     * 个人作品列表
     * @param userId 主页主人的id
     * @param pageNum 页码
     * @param pageSize 一页的记录数
     * @return 返回个人作品卡片
     */
    @Override
    public PageBean<PostCardVO> listUserPosts(Integer userId, int pageNum, int pageSize) {
        // 查询条数
        long total = newsMapper.countByUserId(userId);
        List<PostCardVO> list = newsMapper.listByUserId(userId, (pageNum - 1) * pageSize, pageSize);
        return new PageBean<>(list, total, pageNum, pageSize);
    }


    /** 编辑个人资料（需登录）：昵称/头像/背景/简介/生日/所在地，生日传 yyyy-MM-dd 或空 */


}
