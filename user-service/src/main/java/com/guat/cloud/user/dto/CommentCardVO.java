package com.guat.cloud.user.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论收藏列表项
 */
@Data
@NoArgsConstructor
public class CommentCardVO {
    private Long id;
    @Schema(description = "评论作者的用户名", example = "构造的小麦")
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String content;
    private Integer likeCount;
    private Long newsId;
    /** 所属帖子标题 */
    private String postTitle;
    private LocalDateTime createTime;
}
