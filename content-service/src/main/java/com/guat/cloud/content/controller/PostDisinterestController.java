package com.guat.cloud.content.controller;

import com.guat.cloud.content.dto.Result;
import com.guat.cloud.content.service.PostDisinterestService;
import com.guat.cloud.content.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 帖子"不感兴趣"接口（个人 feed 过滤）
 */
@Tag(name = "不感兴趣接口", description = "帖子标记不感兴趣（feed 过滤）")
@RestController
//@RequestMapping("/api/web/post")
@SecurityRequirement(name = "BearerAuth")
public class PostDisinterestController {

    @Autowired
    private PostDisinterestService postDisinterestService;

    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"isDisliked": true
    //	}
    //}
    /** 不感兴趣 / 取消（需登录） */
    @PostMapping("/api/web/post/disinterest")
    @Operation(summary = "不感兴趣 / 取消", description = "传入 postId，标记该帖子不感兴趣或取消")
    @Parameter(name = "postId", description = "帖子ID", required = true, example = "1")
    public Result<Map<String, Object>> disinterest(@RequestParam Integer postId, HttpServletRequest request) {
        return Result.success(postDisinterestService.toggle(
                UserContext.requireUserId(request), postId));
    }

    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": [
    //		15
    //	]
    //}
    /** 我标记过"不感兴趣"的帖子 id 集合（需登录，feed 过滤用） */
    @GetMapping("/api/web/post/disinterest/ids")
    @Operation(summary = "我标记过不感兴趣的帖子ID集合", description = "返回当前用户标记过不感兴趣的帖子ID列表")
    public Result<List<Long>> disinterestIds(HttpServletRequest request) {
        return Result.success(postDisinterestService.getMyDisinterestIds(
                UserContext.requireUserId(request)));
    }
}
