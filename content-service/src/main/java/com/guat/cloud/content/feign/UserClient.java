package com.guat.cloud.content.feign;

import com.guat.cloud.content.dto.Result;
import com.guat.cloud.content.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// @FeignClient：声明"我要调哪个服务"，name = Nacos 注册的服务名（一字不差）
@FeignClient(name = "user-service")
public interface UserClient {

    // @GetMapping：声明"发什么请求到哪个路径"——和 InnerUserController 完全一致
    // @PathVariable("id")：把方法参数填进 URL 的 {id} 占位符
    @GetMapping("/publicApi/inner/user/{id}")
    Result<UserInfo> getUserInfo(@PathVariable Long id);

    // 批量接口
    @PostMapping("/publicApi/inner/user/batch")
    Result<List<UserInfo>> batchGetUserInfo(@RequestBody List<Long> ids);
}
