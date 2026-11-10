package com.guat.cloud.user.utils;

import java.security.SecureRandom;

/**
 * 随机昵称 / 账号 / 验证码生成工具
 * 规则：昵称 = "用户" + 4位随机字符（排除易混淆 0/O/1/I/l）
 *      账号 = 11位随机数字（首位 1-9，其余 0-9）
 *      验证码 = 6位数字
 */
public final class RandomAccountUtil {

    /** 去掉 0/O/1/I/l 的字母+数字集 */
    private static final char[] NICK_CHARS =
            "abcdefghijkmnpqrstuvwxyz23456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomAccountUtil() {
    }

    /** 生成昵称，如：用户a3k9 */
    public static String generateNickname() {
        StringBuilder sb = new StringBuilder("用户");
        for (int i = 0; i < 4; i++) {
            sb.append(NICK_CHARS[RANDOM.nextInt(NICK_CHARS.length)]);
        }
        return sb.toString();
    }

    /** 生成 11 位数字账号，如：58213394760 */
    public static String generateAccount() {
        StringBuilder sb = new StringBuilder(11);
        sb.append(1 + RANDOM.nextInt(9)); // 首位 1-9
        for (int i = 0; i < 10; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /** 生成 6 位数字短信验证码 */
    public static String generateSmsCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
