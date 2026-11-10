package com.guat.cloud.content.service;

import com.guat.cloud.content.entity.Comment;
import com.guat.cloud.content.dto.PageBean;

import java.util.List;
import java.util.Map;

public interface CommentService {

    //查询一级评论（currentUserId 为当前登录用户ID，可为 null）
    PageBean<Comment> getCommentByID(Integer id, int pageNum, int pageSize, Integer currentUserId);

    //查询子评论（currentUserId 为当前登录用户ID，可为 null）
    PageBean<Comment> getChildComment(Integer newsId, Integer parentId, int pageNum, int pageSize, Integer currentUserId);

    //发表评论
    boolean addComment(Comment comment);

    //删除评论
    boolean delComment(Long id, Long userId);

    /** 点赞/取消点赞评论，返回 {likeCount, isLiked} */
    Map<String, Object> toggleLike(Integer userId, Long commentId);

    /** 不喜欢/取消不喜欢（折叠），返回 {isDisliked} */
    Map<String, Object> toggleDislike(Integer userId, Long commentId);

    /** 查询当前用户已折叠的评论 id 集合（评论列表初始化时用） */
    List<Long> getFoldedCommentIds(Long userId, List<Long> commentIds);
}
