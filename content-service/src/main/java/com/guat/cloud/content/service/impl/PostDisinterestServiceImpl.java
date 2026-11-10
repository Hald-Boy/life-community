package com.guat.cloud.content.service.impl;

import com.guat.cloud.content.entity.PostDisinterest;
import com.guat.cloud.content.exception.BusinessException;
import com.guat.cloud.content.mapper.NewsMapper;
import com.guat.cloud.content.mapper.PostDisinterestMapper;
import com.guat.cloud.content.service.PostDisinterestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostDisinterestServiceImpl implements PostDisinterestService {

    @Autowired
    private PostDisinterestMapper postDisinterestMapper;

    @Autowired
    private NewsMapper newsMapper;

    /**
     * 不感兴趣 / 取消（需登录）
     * @param userId 用户id
     * @param postId 帖子id
     * @return 是否插入/修改不感兴趣记录
     */
    @Override
    public Map<String, Object> toggle(Integer userId, Integer postId) {
        // 根据id查找对应帖子
        if (newsMapper.findById(postId) == null) {
            throw new BusinessException("帖子不存在");
        }
        // 待会儿作为返回值
        boolean isDisliked;
        // 根据用户id和帖子id查询是否存在不感兴趣记录
        PostDisinterest record = postDisinterestMapper.findByUserAndPost(userId.longValue(), postId.longValue());
        if (record == null) {
            PostDisinterest pd = new PostDisinterest();
            // 设置用户id
            pd.setUserId(userId.longValue());
            // 设置帖子id
            pd.setPostId(postId.longValue());
            // 设置状态有效
            pd.setIsCancel(0);
            // 插入不感兴趣记录
            postDisinterestMapper.insert(pd);
            // 设置为返回真
            isDisliked = true;
        } else if (Integer.valueOf(1).equals(record.getIsCancel())) {
            // 如果已存在不感兴趣记录但已取消，重新设置状态为有效
            postDisinterestMapper.updateCancel(record.getId(), 0);
            // 设置返回真
            isDisliked = true;
        } else {
            // 如果已经存在不感兴趣记录并且为有效状态，则返回false
            postDisinterestMapper.updateCancel(record.getId(), 1);
            isDisliked = false;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("isDisliked", isDisliked);
        return result;
    }

    /**
     * 前用户不感兴趣的帖子 id 集合（feed 过滤用）
     * @param userId 用户id
     * @return 返回集合
     */
    @Override
    public List<Long> getMyDisinterestIds(Integer userId) {
        List<Long> ids = postDisinterestMapper.findDisinterestPostIds(userId.longValue());
        return ids == null ? Collections.emptyList() : ids;
    }
}
