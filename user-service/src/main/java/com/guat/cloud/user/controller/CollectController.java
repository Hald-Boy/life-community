package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.CommentCardVO;
import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.PostCardVO;
import com.guat.cloud.user.dto.Result;
import com.guat.cloud.user.service.CollectService;
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
 * 收藏接口（帖子 + 评论）
 */
@Tag(name = "收藏接口", description = "帖子/评论的收藏、取消收藏、状态查询、我的收藏列表")
@RestController
//@RequestMapping("/api/web/collect")
@SecurityRequirement(name = "BearerAuth")
public class CollectController {

    @Autowired
    private CollectService collectService;

    /** 收藏 / 取消收藏（需登录），collectType 1帖子 2评论 */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"isCollected": false
    //	}
    //}
    @PostMapping("/api/web/collect/toggle")
    @Operation(summary = "收藏 / 取消收藏", description = "collectType：1帖子 2评论；targetId 为帖子或评论ID")
    @Parameters({
            @Parameter(name = "collectType", description = "收藏类型：1帖子 2评论", required = true, example = "1"),
            @Parameter(name = "targetId", description = "目标帖子/评论ID", required = true, example = "1")
    })
    public Result<Map<String, Object>> toggle(@RequestParam Integer collectType,
                                              @RequestParam Long targetId,
                                              HttpServletRequest request) {
        return Result.success(collectService.toggleCollect(
                UserContext.requireUserId(request), collectType, targetId));
    }

    /** 收藏状态（无需登录，未登录视为未收藏） */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"isCollected": true
    //	}
    //}
    @GetMapping("/api/web/collect/status")
    @Operation(summary = "查询收藏状态", description = "collectType：1帖子 2评论；返回是否已收藏")
    @Parameters({
            @Parameter(name = "collectType", description = "收藏类型：1帖子 2评论", required = true, example = "1"),
            @Parameter(name = "targetId", description = "目标帖子/评论ID", required = true, example = "1")
    })
    public Result<Map<String, Object>> status(@RequestParam Integer collectType,
                                              @RequestParam Long targetId,
                                              HttpServletRequest request) {
        return Result.success(collectService.getCollectStatus(
                UserContext.getUserId(request), collectType, targetId));
    }

    /** 我的收藏-帖子（需登录） */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"list": [
    //			{
    //				"id": 11,
    //				"title": "今天遇到一件开心的事情",
    //				"content": "这是内容：Content-Type未手动设置，浏览器自动生成multipart/form-data和后端@RequestPart匹配",
    //				"userName": "张三xxx",
    //				"likeCount": 1,
    //				"commentCount": 15,
    //				"collectCount": 1,
    //				"cover": "http://localhost:8080/uploads/news/1783853157669_3536.jpg",
    //				"createTime": "2025-12-10T10:05:15"
    //			}
    //		],
    //		"total": 1,
    //		"pageNum": 1,
    //		"pageSize": 10
    //	}
    //}
    @GetMapping("/api/web/collect/mine/posts")
    @Operation(summary = "我的收藏-帖子", description = "分页返回我收藏的帖子列表")
    @Parameters({
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10")
    })
    public Result<PageBean<PostCardVO>> minePosts(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  HttpServletRequest request) {
        return Result.success(collectService.listMyCollectedPosts(
                UserContext.requireUserId(request), pageNum, pageSize));
    }

    /** 我的收藏-评论（需登录） */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"list": [
    //			{
    //				"id": 2,
    //				"username": "黄中武",
    //				"content": "对在哪？",
    //				"likeCount": 0,
    //				"newsId": 11,
    //				"postTitle": "今天遇到一件开心的事情",
    //				"createTime": "2026-08-26T15:23:59"
    //			}
    //		],
    //		"total": 1,
    //		"pageNum": 1,
    //		"pageSize": 10
    //	}
    //}
    @GetMapping("/api/web/collect/mine/comments")
    @Operation(summary = "我的收藏-评论", description = "分页返回我收藏的评论列表")
    @Parameters({
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10")
    })
    public Result<PageBean<CommentCardVO>> mineComments(@RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize,
                                                          HttpServletRequest request) {
        return Result.success(collectService.listMyCollectedComments(
                UserContext.requireUserId(request), pageNum, pageSize));
    }
}
