package com.guat.cloud.content.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "编辑帖子-需要保留的原有媒体")
public class MediaKeepDTO {
    @Schema(description = "媒体id（news_image表主键，旧媒体才有）")
    private Long id;
    @Schema(description = "排序序号")
    private Integer sortOrder;
}
