package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.Result;
import com.guat.cloud.user.dto.UserInfo;
import com.guat.cloud.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class InnerUserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 免登录获取用户信息，给 content 用
     * @param id 用户id
     * @return 返回用户信息，不含密码
     */
    @GetMapping("/publicApi/inner/user/{id}")
    public Result<UserInfo> getUserInfo(@PathVariable Long id){
        UserInfo userInfo = userMapper.getUserById(id);
        return  Result.success(userInfo);
    }

    /**
     * 批量接口（一次 HTTP 拿全部）
     * 循环外一次性把所有作者 id 打包发给 user-service，【一次 HTTP 拿回 10 个用户】
     * @param ids id集合
     * @return 用户集合
     */
    @PostMapping("/publicApi/inner/user/batch")
    public Result<List<UserInfo>> batchGetUserInfo(@RequestBody List<Long> ids){
        List<UserInfo> users = new ArrayList<>();
        for(Long id:ids){
            UserInfo u = userMapper.getUserById(id);
            if(u!=null){
                users.add(u);
            }
        }
        return Result.success(users);
    }

    /**
     * 测试熔断的接口
     */
    @GetMapping("/publicApi/inner/test/slow")
    public String testSlow() throws InterruptedException {
        Thread.sleep(3000);   // 故意睡 3 秒，模拟慢调用
        return "user-service 正常响应";
    }
}
