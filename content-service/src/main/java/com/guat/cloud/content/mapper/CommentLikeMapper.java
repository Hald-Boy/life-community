package com.guat.cloud.content.mapper;

import com.guat.cloud.content.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface CommentLikeMapper {

    //根据用户id和评论id查询是否已存在点赞记录
    CommentLike findByUserAndComment(@Param("userId") Long userId, @Param("commentId") Long commentId);

    int insert(CommentLike commentLike);

    int updateCancel(@Param("id") Long id, @Param("isCancel") Integer isCancel);

    /** 查询某条评论的全部点赞历史记录（定时任务 diff 用） */
    List<CommentLike> selectByCommentId(@Param("commentId") Long commentId);

    /** 查询全表所有有效点赞记录（启动预热用） */
    List<CommentLike> selectAllValidLikes();
}
