package com.guat.cloud.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "评论实体（新增评论请求体，查询时返回）")
public class Comment {
    private Long id;//评论主键id
    private Long newsId;//关联帖子的id
    private Long userId;//关联用户的id
    private Long parentId;//父评论id
    private Long toUserId;//回复的用户的id
    private String content;//内容
    private Integer status;//评论的状态（是否已经删除）---> 0删除，1正常
    private Long rootCommentId; // ✅ 新增：根一级评论id
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//最后修改时间
    private Integer likeCount;

    // 额外封装字段，数据库不存在，用于前端展示
    private String username; //回复者
    private String avatar;//回复者的头像地址
    private String toUserName;//被回复的用户
    private Integer children; // 子评论列表
    /** 当前登录用户是否已点赞（查询时按 currentUserId 附带，未登录/未点赞为 false） */
    private Boolean liked;
}
