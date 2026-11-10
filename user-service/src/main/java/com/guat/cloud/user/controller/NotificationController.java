package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.Result;
import com.guat.cloud.user.entity.Notification;
import com.guat.cloud.user.service.NotificationService;
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
 * 站内通知接口（需登录）
 */
@Tag(name = "站内通知接口", description = "通知列表、未读数、标记已读")
@RestController
//@RequestMapping("/api/web/notification")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /** 我的通知分页列表 */
    @GetMapping("/api/web/notification/list")
    @Operation(summary = "我的通知分页列表", description = "分页返回当前用户的通知")
    @Parameters({
            @Parameter(name = "pageNum", description = "页码，默认1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认10", required = false, example = "10")
    })
    public Result<PageBean<Notification>> list(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               HttpServletRequest request) {
        return Result.success(notificationService.listMyNotifications(
                UserContext.requireUserId(request), pageNum, pageSize));
    }

    /** 未读数（红点） */
    @GetMapping("/api/web/notification/unread-count")
    @Operation(summary = "未读通知数", description = "返回未读通知数量（红点）")
    public Result<Long> unreadCount(HttpServletRequest request) {
        return Result.success(notificationService.unreadCount(UserContext.requireUserId(request)));
    }

    /** 标记已读：notificationId 为空 = 全部已读 */
    @PostMapping("/api/web/notification/read")
    @Operation(summary = "标记已读", description = "传入 notificationId 标记单条已读；不传则全部已读")
    @Parameter(name = "notificationId", description = "通知ID，不传=全部已读", required = false, example = "1")
    public Result<Void> read(@RequestParam(required = false) Long notificationId,
                             HttpServletRequest request) {
        notificationService.markRead(UserContext.requireUserId(request), notificationId);
        return Result.success();
    }
}
