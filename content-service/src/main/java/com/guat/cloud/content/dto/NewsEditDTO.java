package com.guat.cloud.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 编辑帖子时接收参数的实体
 */
@Schema(description = "编辑帖子时作为请求体）")
@Data
public class NewsEditDTO {
    @Schema(description = "帖子ID，必填")
    private Long id;
    @Schema(description = "帖子标题")
    private String title;
    @Schema(description = "帖子正文")
    private String content;
}
