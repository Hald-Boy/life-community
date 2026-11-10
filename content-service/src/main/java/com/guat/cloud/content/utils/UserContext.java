package com.guat.cloud.content.utils;

import com.guat.cloud.content.dto.Result;
import com.guat.cloud.content.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 从请求域获取用户id
 */
public class UserContext {

    /** 请求域中存放当前登录用户 id 的 key（与现有 LoginInterceptor 一致） */
    public static final String REQUEST_USER_ID = "userId";

    /** 请求域中存放当前登录用户角色（role）的 key（与现有 LoginInterceptor 一致） */
    public static final String REQUEST_ROLE = "role";

    /**
     * 可选获取用户ID：拿不到返回null，不抛异常
     * 接口支持游客访问，登录才拿到用户 id，不登录也可以正常访问接口。
     * 举例：查看帖子详情。游客可以看；登录用户看的时候额外标记是否点赞。
     * @param request 请求头
     * @return 返回用户id
     */
    public static Integer getUserId(HttpServletRequest request) {
        // 从请求域读取拦截器存放的 userId
        // 如果用户**没登录** → 返回null
        // 如果用户**已经登录** → 返回存入的用户 id 对象
        Object uid = request.getAttribute(REQUEST_USER_ID);
        return uid == null ? null : Integer.valueOf(uid.toString());
    }

    /**
     * 必须获取用户ID：拿不到直接抛出未登录异常
     */
    public static Integer requireUserId(HttpServletRequest request) {
        //内部先调用上面的getUserId();
        Integer userId = getUserId(request);
        if (userId == null) throw new BusinessException(Result.CODE_UNAUTHORIZED, "请先登录");
        return userId;
    }

    /** 获取角色 role（未登录/无角色返回 null）；0 普通用户 1 管理员 */
    public static Integer getRole(HttpServletRequest request) {
        Object role = request.getAttribute(REQUEST_ROLE);
        return role == null ? null : Integer.valueOf(role.toString());
    }

    /** 校验管理员（role=1），未登录或非管理员抛 401 */
    public static void requireAdmin(HttpServletRequest request) {
        requireUserId(request);
        if (!Integer.valueOf(1).equals(getRole(request))) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "无管理员权限");
        }
    }

}
