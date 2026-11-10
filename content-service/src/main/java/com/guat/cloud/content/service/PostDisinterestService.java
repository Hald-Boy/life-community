package com.guat.cloud.content.service;

import java.util.List;
import java.util.Map;

public interface PostDisinterestService {

    /** 不感兴趣/取消，返回 {isDisliked} */
    Map<String, Object> toggle(Integer userId, Integer postId);

    /** 当前用户不感兴趣的帖子 id 集合（feed 过滤用） */
    List<Long> getMyDisinterestIds(Integer userId);
}