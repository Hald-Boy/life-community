package com.guat.cloud.content.interceptor;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：身份已由网关统一认证（AuthGlobalFilter 校验 JWT 并转发 X-User-Id / X-User-Role）
 * 这里只信任网关转发的请求头，不再自己解析 JWT
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    //目标资源运行前运行，返回：true放行，返回：false不放行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){

        // 从网关转发的请求头取用户 id（网关已校验过 token，这里不用再解析）
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            throw new JwtException("缺少有效的认证信息");
        }
        // 存入请求域，供 UserContext / Controller 使用（保持原有约定不变）
        request.setAttribute("userId", Integer.valueOf(userId));

        // 角色同样信任网关转发
        String role = request.getHeader("X-User-Role");
        if (role != null && !role.isEmpty()) {
            request.setAttribute("role", Integer.valueOf(role));
        }

        return true;
    }
}
