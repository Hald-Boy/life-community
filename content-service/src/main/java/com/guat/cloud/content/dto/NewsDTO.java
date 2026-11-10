package com.guat.cloud.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 新增帖子时接收参数的实体
 */
@Data
@Schema(description = "新增帖子时作为请求体")
public class NewsDTO {
    private String title;   //新闻标题
    private String content;     //新闻内容
}
