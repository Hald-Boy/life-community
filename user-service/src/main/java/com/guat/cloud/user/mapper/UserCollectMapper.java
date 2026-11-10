package com.guat.cloud.user.mapper;

import com.guat.cloud.user.dto.CommentCardVO;
import com.guat.cloud.user.dto.PostCardVO;
import com.guat.cloud.user.entity.UserCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCollectMapper {

    /**
     * 收藏记录
     * @param userId 用户id
     * @param collectType 收藏的类型 1帖子，2评论
     * @param targetId 收藏目标id
     * @return 收藏对象
     */
    UserCollect findByUserAndTarget(@Param("userId") Long userId,
                                    @Param("collectType") Integer collectType,
                                    @Param("targetId") Long targetId);

    /**
     * 插入收藏记录
     * @param userCollect 收藏对象
     * @return 受影响条数
     */
    int insert(UserCollect userCollect);

    /**
     * 收藏/取消收藏
     * @param id id
     * @param isCancel 0有效 1取消
     * @return 受影响条数
     */
    int updateCancel(@Param("id") Long id, @Param("isCancel") Integer isCancel);

    /** 我的收藏-帖子数 */
    int countCollectedPosts(@Param("userId") Long userId);

    /** 我的收藏-评论数 */
    int countCollectedComments(@Param("userId") Long userId);

    /** 我的收藏-帖子列表（分页） */
    List<PostCardVO> listCollectedPosts(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /** 我的收藏-评论列表（分页） */
    List<CommentCardVO> listCollectedComments(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);
}
