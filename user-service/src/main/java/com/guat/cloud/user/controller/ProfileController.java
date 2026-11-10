package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.*;
import com.guat.cloud.user.service.ProfileService;
import com.guat.cloud.user.service.UserService;
import com.guat.cloud.user.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 个人主页接口
 */
@Tag(name = "个人主页接口", description = "个人主页信息、我的作品列表、编辑个人资料")
@RestController
//@RequestMapping("/api/web/user")
@SecurityRequirement(name = "BearerAuth")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserService userService;

    /**
     * 个人主页（无需登录，未登录 isFollowing=false；看自己也走这里）
     * @param userId 个人主页的所属id
     * @param request 查看主页那个人的id
     * @return 返回个人主页信息
     */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"userInfo": {
    //			"id": 4,
    //			"username": "黄中武",
    //			"userAccount": "34567891",
    //			"phone": "19237837944",
    //			"avatar": null,
    //			"cover": null,
    //			"bio": null,
    //			"location": null,
    //			"birthday": null,
    //			"totalLikeCount": null,
    //			"followCount": 1,
    //			"fanCount": 1,
    //			"role": 1
    //		},
    //		"postCount": 0,
    //		"isFollowing": true,
    //		"isMutual": true
    //	}
    //}
    @GetMapping("/api/web/user/profile")
    @Operation(summary = "个人主页信息", description = "传入 userId，返回个人主页信息（含关注状态）")
    @Parameter(name = "userId", description = "主页所属用户ID", required = true, example = "1")
    public Result<UserProfileVO> profile(@RequestParam Integer userId, HttpServletRequest request) {
        return Result.success(profileService.getProfile(UserContext.getUserId(request), userId));
    }


    /**
     * 我的作品列表（无需登录）
     * @param userId 作品列表的主人id
     * @param pageNum 页码
     * @param pageSize 一页记录数
     * @return 返回作品列表
     */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"list": [],
    //		"total": 0,
    //		"pageNum": 1,
    //		"pageSize": 10
    //	}
    //}
    @GetMapping("/api/web/user/posts")
    @Operation(summary = "我的作品列表", description = "传入 userId，分页返回该用户的帖子列表")
    @Parameters({
            @Parameter(name = "userId", description = "作品列表主人ID", required = true, example = "1"),
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10")
    })
    public Result<PageBean<PostCardVO>> posts(@RequestParam Integer userId,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(profileService.listUserPosts(userId, pageNum, pageSize));
    }

    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"id": 12,
    //		"username": "庞媛媛",
    //		"userAccount": "43964822138",
    //		"phone": "19237837999",
    //		"avatar": null,
    //		"cover": null,
    //		"bio": "世中逢尔，胜过百个泛泛之交",
    //		"location": "广东深圳",
    //		"birthday": null,
    //		"totalLikeCount": null,
    //		"followCount": 1,
    //		"fanCount": 1,
    //		"role": 1
    //	}
    //}
    /** 编辑个人资料（需登录）：昵称/头像/背景/简介/生日/所在地，生日传 yyyy-MM-dd 或空 */
    @PostMapping("/api/web/user/profile/update")
    @Operation(summary = "编辑个人资料", description = "修改昵称/头像/背景/简介/生日/所在地；生日格式 yyyy-MM-dd")
    public Result<UserInfo> updateProfile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "个人资料（JSON）：username/avatar/cover/bio/location/birthday 等", required = true)
            @RequestBody UserInfo userinfo, HttpServletRequest request) {
        return Result.success(userService.updateProfile(UserContext.requireUserId(request), userinfo));
    }
}
