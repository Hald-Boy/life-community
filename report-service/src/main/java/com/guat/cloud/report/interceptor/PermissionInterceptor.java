package com.guat.cloud.report.interceptor;

import com.guat.cloud.report.annotation.RequiredRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

//权限拦截器
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            // 获取被调用方法
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            // 检查方法是否有RequiredRole注解
            RequiredRole requiredRole = method.getAnnotation(RequiredRole.class);
            if (requiredRole != null) {
                // 从注解获取所需角色，例如方法上@RequiredRole(1)
                int role = requiredRole.value();
                // 从网关转发的请求头取角色，不再解析JWT
                String userRoleStr = request.getHeader("X-User-Role");
                if (userRoleStr == null || userRoleStr.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
                int userRole = Integer.parseInt(userRoleStr);
                if (userRole != role) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            }
        }
        return true;
    }

//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        log.info("执行权限控制拦截器的postHandle方法");
//    }

//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        log.info("执行权限控制拦截器的afterCompletion方法");
//    }
}