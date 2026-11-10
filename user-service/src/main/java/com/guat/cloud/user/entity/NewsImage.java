package com.guat.cloud.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsImage {
    private Integer id;                 // 图片ID
    private Integer newsId;             // 关联新闻ID
    private String imageUrl;            // 图片URL
    private Integer sortOrder;          // 排序序号
    private LocalDateTime createTime;   // 创建时间
    private Integer mediaId;            // 媒体类型：1图片，2视频，3混合
    private String coverUrl;            // 封面URL（仅视频使用）
    private Integer width;              // 宽度
    private Integer height;             //高度
    private String isDeleted;           // 逻辑删除标记：1已删除，null/0未删除
}