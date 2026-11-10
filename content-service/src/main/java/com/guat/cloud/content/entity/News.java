package com.guat.cloud.content.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "新闻实体（新增/修改时作为请求体，查询时返回）")
public class News {
    private Integer id;     //新闻id
    private String title;   //新闻标题
    private String content;     //新闻内容
    private Integer userId;     //作者ID
    private String userName;//作者名称
    /** 作者头像URL（列表/详情查询冗余字段，不在 news 表） */
    private String avatar;
    /** 作者账号（列表/详情查询冗余字段，不在 news 表） */
    private String userAccount;
    private LocalDateTime createTime;   //新闻发布时间
    private String coverImageUrl; //封面图片 URL，，，，，，，，，，，，，图片表 关联新闻的第一张图片
    private List<NewsImage> images;    //图片列表
    private Integer viewCount;
    /** 点赞数（冗余字段） */
    private Integer likeCount;
    /** 内容类型：1纯文字 2图文 3视频 4混合 */
    @Schema(description = "内容类型：1纯文字 2图文 3视频 4混合", example = "2")
    private Integer mediaType;
    /** 评论总数（冗余字段） */
    private Integer commentCount;
    /** 收藏总数（冗余字段） */
    private Integer collectCount;
    /** 状态：0 正常 1 封禁 */
    @Schema(description = "状态：0 正常 1 封禁", example = "0")
    private Integer status;
    /** 逻辑删除标记 */
    private String isDeleted;
    private LocalDateTime updateTime;
}
