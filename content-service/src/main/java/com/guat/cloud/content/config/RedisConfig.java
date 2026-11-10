package com.guat.cloud.content.config;

import com.alibaba.fastjson2.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. key 全部用 String 序列化（避免乱码）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 2. 自定义 Fastjson2 序列化器，处理 value
        RedisSerializer<Object> fastjsonSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object object) {
                if (object == null) {
                    return new byte[0];
                }
                // Fastjson2 转 JSON 字节
                return JSON.toJSONBytes(object);
            }

            @Override
            public Object deserialize(byte[] bytes) {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                // Fastjson2 解析回对象
                return JSON.parse(bytes);
            }
        };

        // 3. value 和 hashValue 用 fastjson 序列化
        template.setValueSerializer(fastjsonSerializer);
        template.setHashValueSerializer(fastjsonSerializer);

        // 初始化
        template.afterPropertiesSet();
        return template;
    }
}


