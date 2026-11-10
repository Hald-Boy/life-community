package com.guat.cloud.user.mapper;


import com.guat.cloud.user.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsCodeMapper {

    int insert(SmsCode smsCode);

    /** 查询某手机号在指定场景下最新的一条验证码 */
    SmsCode findLatestByPhoneAndScene(@Param("phone") String phone, @Param("scene") Integer scene);

    /** 标记验证码已使用（防重放） */
    int markUsed(@Param("id") Long id);
}
