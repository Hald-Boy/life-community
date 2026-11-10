package com.guat.cloud.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsSendDTO {

    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "验证码使用场景：1注册 2登录", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "使用场景不能为空")
    private Integer scene;//验证码使用场景 1注册 2登录
}
