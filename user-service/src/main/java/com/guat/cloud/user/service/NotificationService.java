package com.guat.cloud.user.service;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.entity.Notification;

public interface NotificationService {

    /** 我的通知分页列表 */
    PageBean<Notification> listMyNotifications(Integer userId, int pageNum, int pageSize);

    /** 未读数（前端红点） */
    long unreadCount(Integer userId);

    /** 标记已读：notificationId 为空 = 全部已读 */
    void markRead(Integer userId, Long notificationId);

    /** 给指定用户写一条站内通知（供审核等模块调用，与调用方同事务） */
    void notifyUser(Long toUserId, int type, String content, Integer targetType, Long targetId);
}
