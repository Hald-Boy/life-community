package com.guat.cloud.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置：让前端能访问项目根目录的uploads文件夹
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 关键配置：
        // 1. 前端访问 /uploads/** 路径 → 映射到项目根目录的 uploads/ 文件夹
        // 2. "file:./uploads/" 中：
        //    - file: 表示访问本地文件系统（非项目classpath）
        //    - ./ 表示项目启动的根目录（你的News_CourseDesign根目录）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");

        // 可选：如果后续改了存储路径，这里只需要换 addResourceLocations 的值即可
        // 比如后续存到D盘：.addResourceLocations("file:D:/News_CourseDesign_Images/uploads/");
    }
}
