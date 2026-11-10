package com.guat.cloud.content.controller;

import com.guat.cloud.content.entity.Comment;
import com.guat.cloud.content.dto.PageBean;
import com.guat.cloud.content.dto.Result;
import com.guat.cloud.content.service.CommentService;
import com.guat.cloud.content.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "评论接口", description = "新闻评论的查询、新增、删除、点赞与折叠管理，均需登录后访问")
@RestController
//@RequestMapping("/api/web/comment")
//@SecurityRequirement(name = "BearerAuth")
public class CommentController {

    @Autowired
    CommentService commentService;

    /**
     * 查询某帖子的所有一级评论
     * @param id 传入的帖子ID
     * @return 返回所有一级评论
     */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"list": [
    //			{
    //				"id": 20,
    //				"newsId": 13,
    //				"userId": 12,
    //				"parentId": 0,
    //				"toUserId": null,
    //				"content": "我也不懂",
    //				"status": 1,
    //				"rootCommentId": null,
    //				"createTime": "2026-09-15T16:09:05",
    //				"updateTime": null,
    //				"likeCount": null,
    //				"username": "庞媛媛",
    //				"avatar": null,
    //				"toUserName": null,
    //				"children": 0
    //			}
    //		],
    //		"total": 1,
    //		"pageNum": 1,
    //		"pageSize": 10
    //	}
    //}
    @GetMapping("/publicApi/web/comment/listParent")
    @Operation(summary = "查询一级评论", description = "查询某帖子的所有一级评论，分页返回；登录后可传 currentUserId 附带点赞状态")
    @Parameters({
            @Parameter(name = "id", description = "帖子ID", required = true, example = "1"),
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10"),
            @Parameter(name = "currentUserId", description = "当前登录用户ID（可选，用于附带 liked 点赞状态）", required = false, example = "13")
    })
    public Result<PageBean<Comment>> getAllComment(@RequestParam Integer id,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) Integer currentUserId,
                                                   HttpServletRequest request) {

        PageBean<Comment> pageBean = commentService.getCommentByID(id, pageNum, pageSize, currentUserId);
        return Result.success(pageBean);
    }


    /**
     * 根据帖子ID和父评论ID查找父评论的子评论
     * @param newsId 帖子ID
     * @param parentId 父评论ID
     * @return 返回封装好的子评论
     */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"list": [
    //			{
    //				"id": 2,
    //				"newsId": 11,
    //				"userId": 4,
    //				"parentId": 1,
    //				"toUserId": 3,
    //				"content": "对在哪？",
    //				"status": 1,
    //				"rootCommentId": 1,
    //				"createTime": "2026-08-26T15:23:59",
    //				"updateTime": null,
    //				"likeCount": null,
    //				"username": "黄中武",
    //				"avatar": null,
    //				"toUserName": "麦晓雯",
    //				"children": null
    //			},
    //			{
    //				"id": 4,
    //				"newsId": 11,
    //				"userId": 6,
    //				"parentId": 1,
    //				"toUserId": 3,
    //				"content": "我也认为很对",
    //				"status": 1,
    //				"rootCommentId": 1,
    //				"createTime": "2026-08-26T17:38:24",
    //				"updateTime": null,
    //				"likeCount": null,
    //				"username": "妲己",
    //				"avatar": null,
    //				"toUserName": "麦晓雯",
    //				"children": null
    //			}
    //		],
    //		"total": 2,
    //		"pageNum": 1,
    //		"pageSize": 10
    //	}
    //}
    @GetMapping("/publicApi/web/comment/listChild")
    @Operation(summary = "查询子评论", description = "根据帖子ID和父评论ID查询子评论，分页返回；登录后可传 currentUserId 附带点赞状态")
    @Parameters({
            @Parameter(name = "newsId", description = "帖子ID", required = true, example = "1"),
            @Parameter(name = "parentId", description = "父评论ID", required = true, example = "1"),
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10"),
            @Parameter(name = "currentUserId", description = "当前登录用户ID（可选，用于附带 liked 点赞状态）", required = false, example = "13")
    })
    public Result<PageBean<Comment>> getCommentChild(@RequestParam Integer newsId,
                                                     @RequestParam Integer parentId,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) Integer currentUserId) {

        PageBean<Comment> pageBean = commentService.getChildComment(newsId, parentId, pageNum, pageSize, currentUserId);
        return Result.success(pageBean);
    }


    /**
     * 新增评论接口
     * @param request 请求头，获取当前登录用户的id
     * @param comment 评论对象，封装评论的信息：newsId帖子id、parentId父评论id、toUserId被回复者id、content内容
     * @return 返回评论成功/返回失败
     * {
     * 	    "code": 200,
     * 	    "msg": "success",
     * 	    "data": "评论成功"
     * }
     */
    @PostMapping("/api/web/comment/add")
    @Operation(summary = "新增评论", description = "传入评论内容：newsId 帖子ID、parentId 父评论ID（一级评论传0）、toUserId 被回复者ID（一级评论不传）、content 内容")
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> add(HttpServletRequest request,
                              @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                      description = "评论对象（JSON）：newsId、parentId、toUserId、content", required = true)
                              @RequestBody Comment comment) {
        // 从拦截器存入的request域拿userId
        Integer userIdInt = (Integer) request.getAttribute("userId");
        if (userIdInt == null) {
            return Result.error("用户未登录或Token无效");
        }
         //Integer转Long，适配comment的Long类型userId
        Long userId = Long.valueOf(userIdInt);
        log.info("(帖子Id)newsID:{},(评论的父Id)parentId:{},(对谁说的)toUserId:{},(内容)content:{}", comment.getNewsId(), comment.getParentId(), comment.getToUserId(), comment.getContent());

        if(comment.getNewsId() == null || comment.getContent() == null || comment.getContent().trim().isEmpty()){
            return Result.error("评论内容不能为空");
        }
        //把评论的ID设置为当前登录的用户的ID
        comment.setUserId(userId);
        log.info("(谁评论的)uerId:{}", comment.getUserId());
        boolean res = commentService.addComment(comment);
        return res ? Result.success("评论成功"):Result.error("评论失败！");
    }

    /**
     * 删除评论接口
     *
     * @param id 评论的id
     * @param request 请求参数
     * @return res为T则返回删除成功，为F返回删除失败
     */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": "删除成功！"
    //}
    @DeleteMapping("/api/web/comment/del")
    @Operation(summary = "删除评论", description = "传入评论ID，删除自己的评论（需登录）")
    @Parameter(name = "id", description = "评论ID", required = true, example = "1")
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> del(@RequestParam Long id, HttpServletRequest request) {

        // 从拦截器存入的request域拿userId
        Integer userIdInt = (Integer) request.getAttribute("userId");
        if (userIdInt == null) {
            return Result.error("用户未登录或Token无效");
        }
        //把请求头拿到的Integer类型id转为Comment需要的Long类型id
        Long userId = Long.valueOf(userIdInt);

        boolean res = commentService.delComment(id, userId);
        return res ? Result.success("删除成功！"):Result.error("删除失败！");
    }

    /** 点赞 / 取消点赞评论（需登录） */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"isLiked": true,
    //		"likeCount": 1
    //	}
    //}
    @PostMapping("/api/web/comment/like")
    @Operation(summary = "点赞 / 取消点赞评论", description = "传入 commentId，已赞则取消，未赞则点赞")
    @Parameter(name = "commentId", description = "评论ID", required = true, example = "1")
    @SecurityRequirement(name = "BearerAuth")
    public Result<Map<String,Object>> like(@RequestParam Long commentId, HttpServletRequest request) {
        return Result.success(commentService.toggleLike(UserContext.requireUserId(request), commentId));
    }

    /** 不喜欢 / 取消不喜欢（折叠评论，需登录） */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": {
    //		"isDisliked": true
    //	}
    //}
    @PostMapping("/api/web/comment/dislike")
    @Operation(summary = "不喜欢 / 取消不喜欢评论", description = "传入 commentId，折叠该评论")
    @Parameter(name = "commentId", description = "评论ID", required = true, example = "1")
    @SecurityRequirement(name = "BearerAuth")
    public Result<Map<String,Object>> dislike(@RequestParam Long commentId, HttpServletRequest request) {
        return Result.success(commentService.toggleDislike(UserContext.requireUserId(request), commentId));
    }

    /**
     * 查询当前用户已折叠的评论ID集合
     * @param request 获取登录用户
     * @param commentIds 当前页面所有评论id数组，[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]
     * @return 返回该用户已折叠的评论id列表,实际为3和4，前端传入的和后端输出的对比，相同的就是应该折叠的评论
     * {
     * 	"code": 200,
     * 	"msg": "success",
     * 	"data": [
     * 		3,
     * 		4
     * 	]
     * }
     */
    @PostMapping("/api/web/comment/dislike/list")
    @Operation(summary = "查询已折叠评论ID集合", description = "传入当前页所有评论ID数组，返回该用户已折叠的评论ID列表")
    @SecurityRequirement(name = "BearerAuth")
    public Result<List<Long>> getFoldedCommentIds(HttpServletRequest request,
                                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                          description = "当前页所有评论ID数组，如 [1,2,3]", required = true)
                                                  @RequestBody List<Long> commentIds) {
        // 获取登录用户
        Long userId = Long.valueOf(UserContext.requireUserId(request));
        List<Long> foldedIds = commentService.getFoldedCommentIds(userId, commentIds);
        return Result.success(foldedIds);
    }
}
