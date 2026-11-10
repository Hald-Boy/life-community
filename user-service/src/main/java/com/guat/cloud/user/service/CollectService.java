package com.guat.cloud.user.service;

import com.guat.cloud.user.dto.CommentCardVO;
import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.PostCardVO;

import java.util.Map;

public interface CollectService {

    /** 收藏/取消收藏，collectType 1帖子 2评论，返回 {isCollected, collectCount(仅帖子)} */
    Map<String, Object> toggleCollect(Integer userId, Integer collectType, Long targetId);

    /** 收藏状态，viewerId 为空按未收藏处理 */
    Map<String, Object> getCollectStatus(Integer viewerId, Integer collectType, Long targetId);

    /** 我的收藏-帖子（分页） */
    PageBean<PostCardVO> listMyCollectedPosts(Integer userId, int pageNum, int pageSize);

    /** 我的收藏-评论（分页） */
    PageBean<CommentCardVO> listMyCollectedComments(Integer userId, int pageNum, int pageSize);
}
