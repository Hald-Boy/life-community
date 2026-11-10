package com.guat.cloud.content.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.guat.cloud.content.entity.CommentDislike;
import java.util.List;
@Mapper
public interface CommentDislikeMapper {

    CommentDislike findByUserAndComment(@Param("userId") Long userId, @Param("commentId") Long commentId);

    int insert(CommentDislike commentDislike);

    int updateCancel(@Param("id") Long id, @Param("isCancel") Integer isCancel);

    /** 查询当前用户已折叠(未取消)的评论 id 集合，用于评论列表初始化渲染 */
    List<Long> findFoldedCommentIds(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    /** 统计某条评论的有效"踩"数（热评分扣分用） */
    int countValidByCommentId(@Param("commentId") Long commentId);
}
