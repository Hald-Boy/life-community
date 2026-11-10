package com.guat.cloud.content.controller;

import com.guat.cloud.content.feign.UserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publicApi/demo")
public class DemoSlowController {

    @Autowired
    private UserClient userClient;   // 换成你项目里 Feign 接口的真实名字

    @GetMapping("/slow")
    public String slow() {
        long start = System.currentTimeMillis();
        String result = userClient.testSlow();
        long cost = System.currentTimeMillis() - start;
        return "调用结果: " + result + "，耗时: " + cost + "ms";
    }
}
