package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.Result;
import com.guat.cloud.user.entity.User;
import com.guat.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "注册接口", description = "新用户注册功能，无需授权即可访问")
@Slf4j
@RestController
@RequestMapping("/api")
public class RegistrationController {

    @Autowired
    private UserService userService;

    // 已弃用，新接口在 UserController.java
    @Hidden
    @Operation(
            summary = "新用户注册",
            description = "传入用户基本信息完成注册，默认生成普通用户角色；用户名需唯一，密码建议符合复杂度要求"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，data 为提示信息"),
            @ApiResponse(responseCode = "400", description = "用户名或密码为空或注册失败")
    })
    @PostMapping("/closeApi/registration")
    public Result<String> addUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "注册信息（JSON格式），核心字段：username（用户名）、password（密码）", required = true)
            @RequestBody User user) {
        log.info("user={}", user);
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()
                || user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("用户名或密码不能为空");
        }
        userService.addUser(user);
        return Result.success("注册成功！");
    }
}
