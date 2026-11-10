package com.guat.cloud.user.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短信验证码表 sms_code
 */
@Data
@NoArgsConstructor
public class SmsCode {

    private Long id;
    /** 手机号 */
    private String phone;
    /** 验证码 */
    private String code;
    /** 场景：1 注册 2 登录 */
    private Integer scene;
    /** 过期时间 */
    private LocalDateTime expireTime;
    /** 0 未使用 1 已使用 */
    private Integer status;
    private LocalDateTime createTime;
}
