package com.guat.cloud.user.controller;

import com.guat.cloud.user.annotation.RequiredRole;
import com.guat.cloud.user.dto.*;
import com.guat.cloud.user.entity.User;
import com.guat.cloud.user.service.UserService;
import com.guat.cloud.user.utils.JwtUtils;
import com.guat.cloud.user.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

// 模块标签（Swagger UI 分类）
@Tag(name = "用户管理接口", description = "提供用户分页查询、信息修改、个人信息查询、注销、信息更新等功能，部分接口需携带Token授权")
@Slf4j
@RestController
//@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 2025/10/15 于APIPost测试成功
     * @param username 模糊匹配查询
     * @param role 身份
     * @param createTime 创建时间
     * @param page 页码
     * @param pageSize 记录数
     * @return 返回总记录数和列表
     */
    @Operation(
            summary = "分页查询用户列表",
            description = "支持按用户名模糊查询、角色筛选、创建时间筛选，默认页码1、每页10条数据；仅管理员可调用"
    )
    @Parameters({
            @Parameter(name = "username", description = "用户名（模糊匹配），非必填", required = false, example = "张三"),
            @Parameter(name = "role", description = "用户角色（如1=管理员，0=普通用户），非必填", required = false, example = "0"),
            @Parameter(name = "createTime", description = "用户创建日期（格式：yyyy-MM-dd），非必填", required = false, example = "2025-12-01"),
            @Parameter(name = "page", description = "页码，默认值1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页记录数，默认值10", required = false, example = "10")
    })
    @GetMapping("/api/admin/user/list")
    @RequiredRole(1)
    @SecurityRequirement(name = "BearerAuth")
    public Result<PageBean<User>> getAllUsers(
            @RequestParam(required = false) String username, //关键字模糊查询
            @RequestParam(required = false) Integer role,//身份
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createTime, //指定前端传过来的参数是yyyy-MM-dd
            @RequestParam(defaultValue = "1") Integer page,//设置默认值，下同
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        log.info("username:{},role:{},createTime:{},page:{},pageSize:{}", username, role, createTime, page, pageSize);
        //调用service查询用户数据
        PageBean<User> pageBean = userService.getAllUsers(username,role,createTime,page,pageSize);
        return Result.success(pageBean);
    }



    /**
     * 用户信息修改
     * 2025/10/16    只能修改role之外的数据
     * @param userInfo 封装修改的数据
     * @return 返回提示信息
     */
    @Operation(
            summary = "管理员修改其他用户信息",
            description = "用户id从前端传入；仅管理员可调用，可修改昵称和角色"
    )
    @PostMapping("/api/admin/user/update")
    @RequiredRole(1)
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> updateUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户修改信息（JSON格式）：id 必传，username/role 可选", required = true)
            @RequestBody UserInfo userInfo) {
        //要修改的用户的id由前端传入
        userService.updateUser(userInfo);
        return Result.success("用户信息修改成功！");
    }


    /**
     * 注销   2025/10/10
     * 切换到「Headers」标签页，添加授权请求头 Authorization，Bearer Token值
     * 根据ID查询
     */
    @Operation(
            summary = "注销当前登录用户",
            description = "需在请求头添加Authorization: Bearer {Token}；逻辑删除当前用户账号（is_deleted=1），userId从请求域获取"
    )
    @Parameter(name = "request", description = "请求对象（内含拦截器存入的userId），无需前端传参", hidden = true)
    @DeleteMapping("/api/web/user/deleteCurrent")
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> deleteUserById(HttpServletRequest request) {
        try {
            //从请求域获取拦截器存入的userId
            Integer userId = (Integer) request.getAttribute("userId");
            //根据userId注销当前用户
            userService.deleteUserById(userId);
            return Result.success("注销成功！");
        } catch (Exception e) {
            log.error("注销失败！",e);
            return Result.error("注销失败！");
        }
    }


    @Operation(
            summary = "修改当前登录用户密码",
            description = "需校验旧密码；新密码 BCrypt 加密后入库；userId从请求域获取"
    )
    @PostMapping("/api/web/user/updateCurrent")
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            HttpServletRequest request) {
        userService.changePassword(UserContext.requireUserId(request), dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功！");
    }


    /** 手机号 + 短信验证码注册（注册成功直接返回 token，即自动登录） */
    @Operation(summary = "手机号注册", description = "手机号 + 短信验证码注册，成功直接返回 token（自动登录）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，data 为 {token, user}"),
            @ApiResponse(responseCode = "400", description = "手机号/验证码格式错误或验证码不正确")
    })
    @PostMapping("/publicApi/web/user/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterDTO registerDTO) {

        User user = userService.register(registerDTO.getPhone(), registerDTO.getSmsCode(), registerDTO.getPassWord());
        return loginResult(user);
    }


    /** 手机号 + 密码登录 */
     // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"user": {
    //			"id": 12,
    //			"username": "庞媛媛",
    //			"password": null,
    //			"role": 1,
    //			"createTime": null,
    //			"userAccount": "43964822138",
    //			"phone": "19237837999",
    //			"avatar": null,
    //			"cover": null,
    //			"bio": "世中逢尔，胜过百个泛泛之交",
    //			"birthday": null,
    //			"location": "广东深圳",
    //			"totalLikeCount": null,
    //			"followCount": 1,
    //			"fanCount": 1,
    //			"status": null,
    //			"isDeleted": null,
    //			"updateTime": null
    //		},
    //		"token": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoxLCJpZCI6MTIsInVzZXJuYW1lIjoi5bqe5aqb5aqbIiwiZXhwIjoxNzg5NzQyNjAyfQ.LvpZef1J0xki7E8c4ykeWJIR3B9jxRgYxiUwVbgRwUJXb1kuF_5rfI-e8BtIQA62pZYNWA0ZqjhMqOaudT0HzQ"
    //	}
    //}
    @Operation(summary = "手机号密码登录", description = "手机号 + 密码登录，成功返回 token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，data 为 {token, user}"),
            @ApiResponse(responseCode = "400", description = "手机号或密码错误")
    })
    @PostMapping("/publicApi/web/user/login/password")
    public Result<Map<String, Object>> loginByPassword(@Valid @RequestBody LoginPasswordDTO loginPasswordDTO) {

        User user = userService.loginByPassword(loginPasswordDTO.getPhone(), loginPasswordDTO.getPassWord());
        return loginResult(user);
    }

    /** 手机号 + 短信验证码登录 */
    @Operation(summary = "手机号验证码登录", description = "手机号 + 短信验证码登录，成功返回 token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，data 为 {token, user}"),
            @ApiResponse(responseCode = "400", description = "手机号或验证码错误")
    })
    @PostMapping("/publicApi/web/user/login/sms")
    public Result<Map<String, Object>> loginBySms(@Valid @RequestBody LoginSmsDTO loginSmsDTO) {
        User user = userService.loginBySms(loginSmsDTO.getPhone(),loginSmsDTO.getSmsCode());
        return loginResult(user);
    }

    /** 退出登录：JWT 无状态，前端删除本地 token 即可，这里仅做兼容返回 */
    @Operation(summary = "退出登录", description = "JWT 无状态，前端删除本地 token 即可，本接口仅做兼容返回")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("退出成功");
    }


    /** 获取当前登录用户信息（登录态由拦截器解析 JWT 后写入请求域） */
    @Operation(summary = "获取当前登录用户信息", description = "从请求域读取 userId 查询用户信息")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/api/web/user/info")
    public Result<User> info(HttpServletRequest request) {
        return Result.success(userService.getById(UserContext.requireUserId(request)));
    }

    /**
     * 登录/注册成功：生成 JWT token 返回给前端
     * claims 的 key 与现有 LoginInterceptor 解析时一致：id / username / role
     * @param user 已经插入的数据
     */
    private Result<Map<String, Object>> loginResult(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        //生成jwt
        String token = JwtUtils.generateJwt(claims);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.success(data);
    }

}
