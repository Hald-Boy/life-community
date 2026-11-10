package com.guat.cloud.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作品列表项（帖子卡片）
 */
@Data
@NoArgsConstructor
public class PostCardVO {

    private Integer id;
    private String title;
    private String content;
    /** 作者ID（用于跳转个人主页） */
    private Integer userId;
    private String userName;
    /** 作者头像URL */
    private String avatar;
    /** 作者账号 */
    private String userAccount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    /** 首图 URL（无图则 null） */
    private String cover;
    private LocalDateTime createTime;
}
