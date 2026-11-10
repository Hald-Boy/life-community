package com.guat.cloud.report.mapper;

import com.guat.cloud.report.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.util.List;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    /** 我的通知分页列表（倒序） */
    List<Notification> listByUserId(@Param("userId") Long userId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /** 我的通知总数 */
    long countByUserId(@Param("userId") Long userId);

    /** 未读数 */
    long countUnread(@Param("userId") Long userId);

    /** 单条标记已读（带 userId 防越权，只能读自己的） */
    int updateRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 全部标记已读 */
    int markAllRead(@Param("userId") Long userId);
}
