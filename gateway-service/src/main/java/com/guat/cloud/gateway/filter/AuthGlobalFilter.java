package com.guat.cloud.gateway.filter;

import com.guat.cloud.gateway.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局认证过滤器：统一校验 JWT，校验通过后把 userId 放进请求头转发给下游
 */
@Component   // 注册成 Bean，Spring 自动装配到网关过滤器链
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 白名单：公开接口 + 静态资源（图片），不校验 token
    private static final String[] WHITE_LIST = {
            "/user/publicApi", "/content/publicApi", "/report/publicApi", "/uploads"
    };

    /**
     *
     * @param exchange 全局上下文容器：封装了HTTP请求和响应的所有信息
     * @param chain 代表后续过滤器的执行链，是控制流程继续的放行开关
     * @return 方法返回 Mono<Void> 就代表 处理完了
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 拿到本次请求的路径
        String path = exchange.getRequest().getURI().getPath();

        // 2. 白名单判断：路径以任一前缀开头 → 直接放行
        for (String prefix : WHITE_LIST) {
            if (path.startsWith(prefix)) {
                return chain.filter(exchange);   // 放行：交给下一个过滤器
            }
        }

        // 3. 取 Authorization 请求头（前端登录后都会带）
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        // 4. 没带 或 格式不对（不是 "Bearer " 开头）→ 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        // 5. 去掉 "Bearer " 前缀，取出真正的 token
        String token = authHeader.substring(7);

        try {
            // 6. 用和 user-service 一模一样的密钥解析 token
            Claims claims = JwtUtils.parseJwt(token);

            // 7. 解析成功：把用户 id 塞进新的请求头，转发给下游
            // 创建新的请求：因为 ServerWebExchange是不可变对象，要在原始请求新加"X-User-Id"必须创建一个原始请求的“副本”来修改
            ServerHttpRequest newRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(claims.get("id")))
                    .header("X-User-Role", String.valueOf(claims.get("role")))
                    .build();
            // ServerHttpRequest也是不可变的，把旧 request 替换成新 request
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            // 8. token 过期 / 签名不对 / 解析失败 → 401
            return unauthorized(exchange);
        }
    }

    /**
     * 返回 401 + JSON，告诉前端"未登录或登录已过期"
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        // 设置状态码 401
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // 告诉前端响应是 JSON
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 把 JSON 字符串转成字节，包成 DataBuffer
        String body = "{\"code\":401,\"msg\":\"未登录或登录已过期\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        // 写出响应并结束
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 执行顺序：数字越小越先执行。
     * -100 保证在网关内建过滤器之前跑，认证必须最先做
     */
    @Override
    public int getOrder() {
        return -100;
    }
}