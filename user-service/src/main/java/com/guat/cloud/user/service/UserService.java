package com.guat.cloud.user.service;
import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.UserInfo;
import com.guat.cloud.user.entity.User;

import java.time.LocalDate;

public interface UserService {

    PageBean getAllUsers(String username, Integer role, LocalDate createTime, Integer page, Integer pageSize);


    /**
     * 登录校验
     * @param user 传进来的用户名和密码封装在Emp对象
     * @return 返回一个员工
     */
    User login(User user);

    /**
     * 用户注册
     * @param user 封装用户名和密码
     */
    void addUser(User user);

    /**
     * 用户信息修改
     */
    void updateUser(UserInfo userInfo);


    /**
     * 注销当前登录用户（逻辑删除：is_deleted='1'，保留数据避免关联记录成孤儿）
     */
    void deleteUserById(Integer id);


    void updateUserById(User user);


    /** 手机号 + 短信验证码注册，注册后生成随机昵称/账号 */
    User register(String phone, String smsCode, String password);

    /** 手机号 + 密码登录 */
    User loginByPassword(String phone, String password);

    /** 手机号 + 短信验证码登录（仅限已注册手机号） */
    User loginBySms(String phone, String smsCode);

    User getById(Integer id);

    /** 更新个人资料（昵称/头像/背景/简介/生日/所在地），birthday 传 yyyy-MM-dd 或空 */
    UserInfo updateProfile(Integer userId, UserInfo userinfo);

    /** 修改当前登录用户密码：校验旧密码后，新密码 BCrypt 加密入库 */
    void changePassword(Integer userId, String oldPassword, String newPassword);


}
