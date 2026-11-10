package com.guat.cloud.user.controller;

import com.guat.cloud.user.dto.SmsSendDTO;
import com.guat.cloud.user.dto.Result;
import com.guat.cloud.user.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 短信接口
 */
@Tag(name = "短信接口", description = "发送短信验证码（模拟实现，验证码打印在控制台）")
@RestController
@RequestMapping("/publicApi/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    /**
     * 发送短信验证码（模拟实现，验证码打印在控制台日志）
     * @param smsSendDTO 封装手机号和使用场景参数
     * @return 统一返回格式
     */
    // 响应格式
    // {
    //	"code": 200,
    //	"msg": "success",
    //	"data": "验证码已发送"
    //}
    @Operation(summary = "发送短信验证码", description = "传入手机号和场景，模拟发送，验证码打印在控制台日志")
    @PostMapping("/send")
    public Result<String> send(@io.swagger.v3.oas.annotations.parameters.RequestBody(
                                       description = "手机号 + 使用场景（1注册 2登录）", required = true)
                               @Valid @RequestBody SmsSendDTO smsSendDTO) {
        smsService.sendCode(smsSendDTO.getPhone(), smsSendDTO.getScene());
        return Result.success("验证码已发送");
    }
}
