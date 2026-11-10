package com.guat.cloud.user.config;

import com.guat.cloud.user.interceptor.LoginInterceptor;
import com.guat.cloud.user.interceptor.PermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 配置拦截器的拦截路径
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // addPathPatterns代表要拦截的所有路径，excludePathPatterns("/login")代表唯独不拦截登录路径
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**", "/logout").excludePathPatterns("/publicApi/**").order(1);
        // 指定拦截所有/api/开头的请求路径
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**").order(2);
    }

}
