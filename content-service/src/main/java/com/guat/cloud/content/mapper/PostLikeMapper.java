package com.guat.cloud.content.mapper;

import com.guat.cloud.content.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostLikeMapper {

    //根据用户id和帖子id查询所有点赞记录
    PostLike findByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    //插入新闻点赞记录
    int insert(PostLike postLike);

    //设置点赞记录是否有效
    int updateCancel(@Param("id") Long id, @Param("isCancel") Integer isCancel);

    /**
     * 根据帖子ID查询所有点赞记录
     */
    List<PostLike> selectByPostId(Long postId);

    /**
     * 查询所有有效点赞记录（用于预热）
     */
    List<PostLike> selectAllValidLikes();

}
