package com.guat.cloud.user.service;

/**
 * 短信服务：P0 采用模拟实现（验证码打印日志），接口抽象方便将来切换真实短信平台
 */
public interface SmsService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param scene 1 注册 2 登录
     */
    void sendCode(String phone, Integer scene);

    /**
     * 校验验证码（校验通过即标记已使用，防止重放）
     *
     * @param phone 手机号
     * @param scene 场景
     * @param code  用户输入的验证码
     */
    void verifyCode(String phone, Integer scene, String code);
}
