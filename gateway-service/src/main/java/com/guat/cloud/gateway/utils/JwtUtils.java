package com.guat.cloud.gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtils {

    // 定义JWT过期时间（单位：毫秒），这里43,200,000ms也就是12小时
    private static Long expire = 43200000L;

    /**
     * 固定密钥字符串（HS512 要求密钥 >= 512 bit = 64 字节，ASCII 字符 1 字符 = 1 字节）
     * 上线前务必改成你自己的固定字符串，不要用示例值；改完重启服务后已签发的 token 依然有效
     */
    private static final String SECRET_KEY_STRING =
            "ThisIsAFixedSecretKeyForHS512DoNotShareItChangeItBeforeUseAtLeast64Chars!";

    // 由固定字符串派生签名密钥：服务重启后密钥不变，不会把用户全部踢下线
    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));

    // builder：生成JWT方法，参数接收一个包含声明（claims）的Map
    public static String generateJwt(Map<String, Object> claims) {
        // 使用JWT构造器创建JWT对象
        String jwt = Jwts.builder()
                .addClaims(claims)  //添加声明（claims），如用户信息，自定义属性等
                .signWith(SECRET_KEY)   //使用固定的SECRET_KEY进行签名，保证jwt的完整性和不可篡改性
                .setExpiration(new Date(System.currentTimeMillis() + expire))   //设置过期时间
                .compact();     //将构建好的jwt压缩成紧凑的字符串形式返回
        return jwt;
    }

    // parser：解析jwt的方法，参数接收jwt字符串
    public static Claims parseJwt(String jwt) {
        // 使用JWT解析器构建器创建解析器
        Claims claims = Jwts.parser()   // 设置解析时使用的签名密钥，必须与生成JWT时的密钥一致，否则解析失败
                .setSigningKey(SECRET_KEY) // 这里必须和生成时用同一个密钥！
                .parseClaimsJws(jwt)     // 从解析结果中获取具体的声明内容并返回
                .getBody();
        return claims;
    }
}
