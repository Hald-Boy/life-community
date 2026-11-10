package com.guat.cloud.content.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/test")
    public String test() {
        redisTemplate.opsForValue().set("name", "测试Redis连通");
        String val = (String) redisTemplate.opsForValue().get("name");
        return "redis读取结果：" + val;
    }
}
