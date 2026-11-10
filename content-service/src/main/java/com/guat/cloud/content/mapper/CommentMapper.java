package com.guat.cloud.content.mapper;

import com.guat.cloud.content.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    //查询所有一级评论
    //传入帖子ID；currentUserId 为当前登录用户ID（可为 null，游客不查点赞状态）
    List<Comment> getCommentByNewsId(Integer newsId, int pageNum, int pageSize, Integer currentUserId);
    // 统计一级评论总条数（用于分页total）
    Long countRootComment(Integer newsId);


    //查询帖子的子评论
    //传入帖子ID和父评论ID；currentUserId 为当前登录用户ID（可为 null，游客不查点赞状态）
    List<Comment> getChildComment(Integer newsId, Integer rootCommentId, int pageNum, int pageSize, Integer currentUserId);
    //统计某个父评论下子评论总数
    Long countChildComment(Integer newsId, Integer rootCommentId);



    /**
     * 根据评论id查询单条评论（新增子评论时，用来获取父评论的rootCommentId）
     */
    Comment selectById(@Param("id") Long id);

    // 新增评论
    int insertComment(Comment comment);
    // 逻辑删除评论
    int deleteComment(@Param("id") Long id, @Param("userId") Long userId);

    /** 增减评论点赞数，delta 可为 ±1 */
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 覆盖式设置评论点赞数（Redis 同步用，以 Redis 实际集合大小为准，不做累加） */
    int setLikeCount(@Param("id") Long id, @Param("likeCount") int likeCount);

    /** 按 id 列表批量查评论详情（热评 ZSet 拿到 id 后，再回表查内容） */
    List<Comment> listByIds(@Param("ids") List<Long> ids, @Param("currentUserId") Integer currentUserId);

    /** 预热用：查所有 status=1 的根评论（id/newsId/userId/createTime/likeCount） */
    List<Comment> listAllRootCommentsForWarmUp();

    /** 更新评论状态：status 0 删除(下架) 1 正常 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
