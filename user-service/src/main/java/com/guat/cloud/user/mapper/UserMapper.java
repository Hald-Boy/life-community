package com.guat.cloud.user.mapper;

import com.guat.cloud.user.dto.UserInfo;
import com.guat.cloud.user.entity.News;
import com.guat.cloud.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {


    /**
     * 查询所有用户
     * @return 返回所有用户列表
     */
    @Select("select count(*) from user where is_deleted is null or is_deleted = '0'")
    Long countUsers();
    List<User> selectUsers(String username, Integer role, LocalDate createTime, Integer start, Integer size,LocalDateTime startTime, LocalDateTime endTime);




    /**
     * 登录校验：查询有没有和输入的用户名和密码匹配的员工记录
     * @param user 传进来的用户名和密码封装在Emp对象里
     * @return 返回一个员工，如果emp为空，说明用户名或密码错误，反之则成功
     */
    @Select("select * from user where username = #{username} and password = #{password}")
    User selectLogin(User user);

//    /**
//     * 注册
//     */
    @Insert("insert into user (username, password, role, create_time) values (#{username},#{password},#{role},#{createTime})")
    void addUser(User user);

    /**
     * 修改用户信息
     * @param userInfo .
     */
    void updateUser(UserInfo userInfo);

    /**
     * 用户信息查询
     * @param id 根据ID来
     * @return 返回的数据装在User
     */
    @Select("select * from user where id = #{id}")
    UserInfo getUserById(Long id);


    @Update("update user set is_deleted = '1', update_time = now() where id = #{id}")
    void deleteUserById(Integer id);

    //修改自己信息
    void updateUserById(User user);

    //新

    User findByPhone(@Param("phone") String phone);

    User findByAccount(@Param("userAccount") String userAccount);

    User findById(@Param("id") Integer id);

    int insert(User user);

    /** 增减获赞总数，delta 可为 ±1 */
    int updateTotalLikeCount(@Param("id") Integer id, @Param("delta") int delta);

    /** 增减关注数，delta 可为 ±1 */
    int updateFollowCount(@Param("id") Integer id, @Param("delta") int delta);

    /** 增减粉丝数，delta 可为 ±1 */
    int updateFanCount(@Param("id") Integer id, @Param("delta") int delta);

    /** 更新个人资料（昵称/头像/背景/简介/生日/所在地），birthday 传 yyyy-MM-dd 或空 */
    int updateProfile(Integer userId, UserInfo  userInfo);

    /**
     * 直接设置用户总获赞数（覆盖式）
     */
    void setTotalLikeCount(Long userId, Integer count);

}
