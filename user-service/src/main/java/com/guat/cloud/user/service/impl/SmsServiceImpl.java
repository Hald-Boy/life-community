package com.guat.cloud.user.service.impl;

import com.guat.cloud.user.entity.SmsCode;
import com.guat.cloud.user.exception.BusinessException;
import com.guat.cloud.user.mapper.SmsCodeMapper;
import com.guat.cloud.user.service.SmsService;
import com.guat.cloud.user.utils.RandomAccountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 短信服务模拟实现：验证码打印到控制台日志，正式环境替换为短信平台 SDK 即可
 */
@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    /** 同一手机号同场景发送间隔 */
    private static final long SEND_INTERVAL_MS = 60_000L;
    /** 验证码有效期 */
    private static final long CODE_VALID_MS = 5 * 60_000L;

    @Autowired
    private SmsCodeMapper smsCodeMapper;

    /**
     *发送验证码
     * @param phone 手机号
     * @param scene 1 注册 2 登录
     */
    @Override
    public void sendCode(String phone, Integer scene) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        if (scene == null || (scene != 1 && scene != 2)) {
            throw new BusinessException("场景参数错误");
        }
        // 60 秒防刷
        SmsCode last = smsCodeMapper.findLatestByPhoneAndScene(phone, scene);
        if (last != null && last.getCreateTime() != null) {
            //创建时间和当前时间的时间差
            long gap = Duration.between(last.getCreateTime(), LocalDateTime.now()).toMillis();
            if (gap < SEND_INTERVAL_MS) {
                throw new BusinessException("发送太频繁，请稍后再试");
            }
        }
        //生成验证码
        String code = RandomAccountUtil.generateSmsCode();
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setScene(scene);
        smsCode.setStatus(0);
        smsCode.setExpireTime(LocalDateTime.now().plus(CODE_VALID_MS, ChronoUnit.MILLIS));
        smsCodeMapper.insert(smsCode);
        // 模拟短信：真实环境改为调用阿里云/腾讯云短信 SDK
        log.info("【谷歌信息】验证码={}（5分钟内有效）是您的Google验证码。请勿与任何人分享此码。", code);
    }

    /**
     * 校验验证码
     * @param phone 手机号
     * @param scene 场景
     * @param code  用户输入的验证码
     */
    @Override
    public void verifyCode(String phone, Integer scene, String code) {
        if (code == null || code.isEmpty()) {
            throw new BusinessException("验证码不能为空");
        }
        //根据手机号和使用场景查询最新的一条验证码
        SmsCode last = smsCodeMapper.findLatestByPhoneAndScene(phone, scene);
        if (last == null) {
            throw new BusinessException("请先获取验证码");
        }
        if (last.getStatus() != null && last.getStatus() == 1) {
            throw new BusinessException("验证码已使用，请重新获取");
        }
        if (last.getExpireTime() == null || last.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!last.getCode().equals(code)) {
            throw new BusinessException("验证码错误");
        }
        //以上判断均不满足，即为有效验证码，则设置为“已使用”。
        smsCodeMapper.markUsed(last.getId());
    }
}
