package com.guat.cloud.content.mapper;

import com.guat.cloud.content.entity.PostDisinterest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface PostDisinterestMapper {

    // 根据用户id和帖子id查询是否存在不感兴趣的记录
    PostDisinterest findByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);

    // 插入不感兴趣记录
    int insert(PostDisinterest postDisinterest);

    // 更新不感兴趣记录的状态
    int updateCancel(@Param("id") Long id, @Param("isCancel") Integer isCancel);

    /** 当前用户不感兴趣的帖子 id 集合（feed 过滤用） */
    List<Long> findDisinterestPostIds(@Param("userId") Long userId);
}
