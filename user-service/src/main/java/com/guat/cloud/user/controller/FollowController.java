package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.Result;
import com.guat.cloud.user.dto.UserCardVO;
import com.guat.cloud.user.service.FollowService;
import com.guat.cloud.user.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 关注 / 粉丝接口
 */
@Tag(name = "关注/粉丝接口", description = "关注与取关、关注状态、关注列表、粉丝列表")
@RestController
//@RequestMapping("/api/web/follow")
@SecurityRequirement(name = "BearerAuth")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 关注 / 取消关注（需登录）
     * @param targetUserId 被关注的用户id
     * @param request 获取当前登录用户的id
     * @return 返回是否关注/取消关注成功    true为关注，false为取消关注
     * {
     * 	"code": 200,
     * 	"msg": "success",
     * 	"data": {
     * 		"isFollowing": true
     * 	    }
     * }
     */
    @PostMapping("/api/web/follow/toggle")
    @Operation(summary = "关注 / 取消关注", description = "传入被关注用户ID，返回 {isFollowing}")
    @Parameter(name = "targetUserId", description = "被关注的用户ID", required = true, example = "1")
    public Result<Map<String, Object>> follow(@RequestParam Integer targetUserId, HttpServletRequest request) {
        return Result.success(followService.toggleFollow(UserContext.requireUserId(request), targetUserId));
    }


    /**
     * 关注状态（无需登录，未登录 isFollowing=false）
     * @param targetUserId 目标用户id
     * @param request 当前登录用户id
     * @return 返回是否关注/是否相互关注    isFollowing为是否关注，isMutual为是否相互关注，前端通过true/false渲染“已关注”和“相互关注”
     * {
     * 	"code": 200,
     * 	"msg": "success",
     * 	"data": {
     * 		"isFollowing": true,
     * 		"isMutual": true
     * 	    }
     * }
     */
    @GetMapping("/api/web/follow/status")
    @Operation(summary = "关注状态", description = "传入目标用户ID，返回 {isFollowing, isMutual}")
    @Parameter(name = "targetUserId", description = "目标用户ID", required = true, example = "1")
    public Result<Map<String, Object>> followStatus(@RequestParam Integer targetUserId, HttpServletRequest request) {
        return Result.success(followService.getFollowStatus(UserContext.getUserId(request), targetUserId));
    }


    /**
     * 关注列表（查看 userId 的关注列表，无需登录）
     * @param userId 被查看者的id
     * @param pageNum 页码
     * @param pageSize 一页记录数
     * @param request 获取当前登录用户的id
     * @return 实例：查询自己的关注列表
     * {
     * 	"code": 200,
     * 	"msg": "success",
     * 	"data": {
     * 		"list": [
     * 			            {
     * 				"userInfo": {
     * 					"id": 4,
     * 					"username": "黄中武",
     * 					"userAccount": "34567891",
     * 					"phone": "19237837944",
     * 					"avatar": null,
     * 					"cover": null,
     * 					"bio": null,
     * 					"location": null,
     * 					"birthday": null,
     * 					"totalLikeCount": null,
     * 					"followCount": 1,
     * 					"fanCount": 1,
     * 					"role": 1
     *                },
     * 				"isFollowing": true,
     * 				"isMutual": true
     *            }
     * 		],
     * 		"total": 1,
     * 		"pageNum": 1,
     * 		"pageSize": 10
     * 	}
     * }
     */
    @GetMapping("/api/web/follow/following")
    @Operation(summary = "关注列表", description = "查看 userId 的关注列表，分页返回")
    @Parameters({
            @Parameter(name = "userId", description = "被查看者的用户ID", required = true, example = "1"),
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10")
    })
    public Result<PageBean<UserCardVO>> following(@RequestParam Integer userId,
                                                  @RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  HttpServletRequest request) {
        return Result.success(followService.listFollowing(UserContext.getUserId(request), userId, pageNum, pageSize));
    }


    /**
     * 粉丝列表（查看 userId 的粉丝列表，无需登录）
     * @param userId 被查看者的id
     * @param pageNum 页码
     * @param pageSize 一页记录数
     * @param request 获取当前登录用户的id
     * @return 实例：庞媛媛查询麦晓雯的粉丝列表，黄中武是麦晓雯的粉丝，庞媛媛和黄中武相互关注
     * {
     * 	"code": 200,
     * 	"msg": "success",
     * 	"data": {
     * 		"list": [
     * 			            {
     * 				"userInfo": {
     * 					"id": 4,
     * 					"username": "黄中武",
     * 					"userAccount": "34567891",
     * 					"phone": "19237837944",
     * 					"avatar": null,
     * 					"cover": null,
     * 					"bio": null,
     * 					"location": null,
     * 					"birthday": null,
     * 					"totalLikeCount": null,
     * 					"followCount": 1,
     * 					"fanCount": 1,
     * 					"role": 1
     *                },
     * 				"isFollowing": true,
     * 				"isMutual": true
     *            }
     * 		],
     * 		"total": 1,
     * 		"pageNum": 1,
     * 		"pageSize": 10
     * 	}
     * }
     */
    @GetMapping("/api/web/follow/fans")
    @Operation(summary = "粉丝列表", description = "查看 userId 的粉丝列表，分页返回")
    @Parameters({
            @Parameter(name = "userId", description = "被查看者的用户ID", required = true, example = "1"),
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10")
    })
    public Result<PageBean<UserCardVO>> fans(@RequestParam Integer userId,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               HttpServletRequest request) {
        return Result.success(followService.listFans(UserContext.getUserId(request), userId, pageNum, pageSize));
    }
}
