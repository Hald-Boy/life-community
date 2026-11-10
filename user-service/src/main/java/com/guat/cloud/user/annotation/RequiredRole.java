package com.guat.cloud.user.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个方法级别的注解，用于指定访问该方法所需的角色
 * value参数存储角色 ID（如 1 = 管理员，0 = 普通用户）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredRole {
    int value();
}