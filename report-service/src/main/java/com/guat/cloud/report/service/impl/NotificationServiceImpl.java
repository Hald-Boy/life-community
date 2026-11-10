package com.guat.cloud.report.service.impl;

import com.guat.cloud.report.dto.PageBean;
import com.guat.cloud.report.exception.BusinessException;
import com.guat.cloud.report.mapper.NotificationMapper;
import com.guat.cloud.report.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.guat.cloud.report.entity.Notification;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public PageBean<Notification> listMyNotifications(Integer userId, int pageNum, int pageSize) {
        long total = notificationMapper.countByUserId(userId.longValue());
        List<Notification> list = notificationMapper.listByUserId(
                userId.longValue(), (pageNum - 1) * pageSize, pageSize);
        return new PageBean<>(list, total, pageNum, pageSize);
    }

    /**
     * 获取未读数
     * @param userId 接收通知的用户id（内容作者/举报者）
     * @return 返回未读消息数
     */
    @Override
    public long unreadCount(Integer userId) {
        return notificationMapper.countUnread(userId.longValue());
    }

    /**
     * 标记已读
     * @param userId 接收通知的用户id（内容作者/举报者）
     * @param notificationId 通知id
     */
    @Override
    public void markRead(Integer userId, Long notificationId) {
        if (notificationId == null) {
            // 如果通知id为空，说明要全部已读
            notificationMapper.markAllRead(userId.longValue());
        } else {
            notificationMapper.updateRead(notificationId, userId.longValue());
        }
    }

    /**
     * 通知用户
     * @param toUserId 通知接收者
     * @param type 通知类型：1 内容被下架，2 举报被驳回
     * @param content 内容
     * @param targetType 关联内容的类型：1 帖子，2 评论
     * @param targetId 关联内容的id
     */
    @Override
    public void notifyUser(Long toUserId, int type, String content, Integer targetType, Long targetId) {
        if (toUserId == null) {
            throw new BusinessException("通知接收用户不存在");
        }
        Notification notification = new Notification();
        // 封装数据
        notification.setUserId(toUserId);           // 通知接收者
        notification.setType(type);                 // 通知类型：1 内容被下架，2 举报被驳回
        notification.setContent(content);           // 内容
        notification.setTargetType(targetType);     // 关联内容的类型：1 帖子，2 评论
        notification.setTargetId(targetId);         // 关联内容的id
        notification.setIsRead(0);                  // 0 未读，1 已读
        notificationMapper.insert(notification);    //插入通知记录
    }
}
