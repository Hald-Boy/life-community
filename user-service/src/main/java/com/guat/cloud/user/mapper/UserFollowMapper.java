package com.guat.cloud.user.mapper;

import com.guat.cloud.user.entity.User;
import com.guat.cloud.user.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface UserFollowMapper {

    // 查询 “用户关注表user_follow” 是否已经存在有效的关注记录
    UserFollow findByUserAndFollowUser(@Param("userId") Long userId,
                                       @Param("followUserId") Long followUserId);

    //插入关注记录到 “用户关注表user_follow”
    int insert(UserFollow userFollow);

    //设置对应id的关注状态
    int updateCancel(@Param("id") Long id, @Param("isCancel") Integer isCancel, @Param("oldIsCancel") Integer oldIsCancel);

    /** 某用户有效关注的用户数 */
    int countFollowing(@Param("userId") Long userId);

    /** 某用户的有效粉丝数 */
    int countFans(@Param("followUserId") Long followUserId);

    /** 当前用户关注的全部用户 id 集合 */
    List<Long> findFollowingIds(@Param("userId") Long userId);

    /** 在给定 id 集合中，哪些用户关注了 userId（用于判断互关） */
    List<Long> findMutualTargets(@Param("userId") Long userId,
                                 @Param("targetIds") List<Long> targetIds);

    /** 关注列表（分页，带用户信息） */
    List<User> listFollowing(@Param("userId") Long userId,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    /** 粉丝列表（分页，带用户信息） */
    List<User> listFans(@Param("followUserId") Long followUserId,
                        @Param("offset") int offset,
                        @Param("limit") int limit);
}
