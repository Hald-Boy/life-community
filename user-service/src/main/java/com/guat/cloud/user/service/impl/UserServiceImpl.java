package com.guat.cloud.user.service.impl;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.UserInfo;
import com.guat.cloud.user.entity.User;
import com.guat.cloud.user.exception.BusinessException;
import com.guat.cloud.user.mapper.UserMapper;
import com.guat.cloud.user.service.SmsService;
import com.guat.cloud.user.service.UserService;
import com.guat.cloud.user.utils.RandomAccountUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SmsService smsService;

    private static final String PHONE_REGEX = "^1\\d{10}$";

    private static final DateTimeFormatter BIRTHDAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public PageBean<User> getAllUsers(String username, Integer role, LocalDate createTime,Integer page,Integer pageSize) {


        //获取分页查询的起始索引（修复：原来写成 (page-1)*page，第2页起错位）
        int start = (page - 1) * pageSize;
        PageBean<User> pageBean = new PageBean<>();

        LocalDateTime startTime = null; // 当日00:00:00
        LocalDateTime endTime = null;   // 当日23:59:59
        if(createTime != null) {
            startTime = createTime.atStartOfDay();// 转换为：2025-12-04 00:00:00
            endTime = createTime.atTime(23, 59, 59); // 转换为：2025-12-04 23:59:59
        }

        //调用Mapper接口的方法
        pageBean.setList(userMapper.selectUsers(username,role,createTime,start,pageSize,startTime,endTime)); //用户列表
        pageBean.setTotal(userMapper.countUsers()); //总记录数
        pageBean.setPageNum(page);
        pageBean.setPageSize(pageSize);
        return pageBean;
    }



    /**
     * 登录
     * @param user 传进来的用户名和密码封装在Emp对象
     * @return .
     */
    @Override
    public User login(User user){
        return userMapper.selectLogin(user);
    }

    /**
     * 注册
     * @param user 封装用户信息
     */
    @Override
    public void addUser(User user) {
        user.setCreateTime(LocalDateTime.now());
        user.setRole(0);
        userMapper.addUser(user);
    }

    /**
     * 修改用户信息
     * @param userInfo 封装修改的数据
     */
    @Override
    public void updateUser(UserInfo userInfo) {
        userMapper.updateUser(userInfo);
    }

    /**
     * 注销账号（逻辑删除：is_deleted='1'，保留数据避免关联记录成孤儿）
     * @param id 要删除的用户的ID
     */
    @Override
    public void deleteUserById(Integer id) {
        userMapper.deleteUserById(id);
    }


    /**
     * 修改自己的信息
     * @param user .
     */
    @Override
    public void updateUserById(User user) {
        userMapper.updateUserById(user);
    }


    /**
     * 注册账号
     * @param phone
     * @param smsCode
     * @param password
     * @return
     */
    @Override
    public User register(String phone, String smsCode, String password) {
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }
        if (userMapper.findByPhone(phone) != null) {
            throw new BusinessException("该手机号已注册");
        }
        // 校验短信验证码（scene=1 注册）
        smsService.verifyCode(phone, 1, smsCode);

        User user = new User();
        user.setPhone(phone);
//        // 密码可空 = 纯短信账号；设置了密码才能用"手机号+密码"登录
//        user.setPassword(password == null || password.isEmpty() ? null : password);
        //如果密码不为空，gensalt() 自动生成随机盐，盐内置在返回字符串里面，不需要单独存数据库
        if(password != null || !password.isEmpty() ){
            user.setPassword(BCrypt.hashpw(password,BCrypt.gensalt()));
        }else {
            throw new BusinessException("请输入密码");
        }
        // 随机昵称：用户 + 4位随机字符
        user.setUsername(RandomAccountUtil.generateNickname());
        // 随机账号：11位数字，唯一性重试
        String account;
        int retry = 0;
        do {
            account = RandomAccountUtil.generateAccount();
            retry++;
        } while (userMapper.findByAccount(account) != null && retry < 10);
        //如果连续 10 次全部撞库（生成的账号全都数据库已有）：循环直接退出，此时account依旧可能是重复账号
        //拿最后生成的账号再次查询，如果还是重复了那就报异常
        if (userMapper.findByAccount(account) != null) {
            throw new BusinessException("账号生成失败，请重试");
        }
        user.setUserAccount(account);
        user.setRole(0);
        userMapper.insert(user);
        //插入成功之后，自动设置当前user的id
        return User.from(userMapper.findById(user.getId()));
        //return userMapper.findById(user.getId());
    }

    /**
     * 用密码登录
     * @param phone
     * @param password
     * @return
     */
    @Override
    public User loginByPassword(String phone, String password) {
        if (phone == null || password == null || password.isEmpty()) {
            throw new BusinessException("手机号和密码不能为空");
        }
        //根据手机号查询有没有这个用户
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }
//        if (user.getPassword() == null || !user.getPassword().equals(password)) {
//            throw new BusinessException("密码错误");
//        }
        if(!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return User.from(user);
    }

    /**
     * 用验证码登录
     * @param phone
     * @param smsCode
     * @return
     */
    @Override
    public User loginBySms(String phone, String smsCode) {
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new BusinessException("该手机号未注册，请先注册");
        }
        // 校验短信验证码（scene=2 登录）
        smsService.verifyCode(phone, 2, smsCode);
        return User.from(user);
    }

    /**
     * 根据ID查找用户
     * @param id
     * @return
     */
    @Override
    public User getById(Integer id) {
        return User.from(userMapper.findById(id));
    }


    /**
     * 编辑个人信息
     * @param userId
     * @param userinfo
     * @return
     */
    @Override
    public UserInfo updateProfile(Integer userId, UserInfo userinfo) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updateProfile(userId, userinfo);
        return UserInfo.from(userMapper.findById(userId));
    }

    /**
     * 修改当前登录用户密码：校验旧密码后，新密码 BCrypt 加密入库
     * @param userId 当前登录用户 id
     * @param oldPassword 旧密码（明文，用于校验）
     * @param newPassword 新密码（明文，加密后入库）
     */
    @Override
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getPassword() == null || !BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        User upd = new User();
        upd.setId(userId);
        upd.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userMapper.updateUserById(upd);
    }

    /** 去空格，空串转 null，超长报错 */
    private String trimToNull(String value, int maxLen, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > maxLen) {
            throw new BusinessException(field + "最多" + maxLen + "个字符");
        }
        return v;
    }
}
