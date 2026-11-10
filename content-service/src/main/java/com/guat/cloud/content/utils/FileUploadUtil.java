package com.guat.cloud.content.utils;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class FileUploadUtil {
    // 定义项目根目录的绝对路径
    private String baseDir;    //已修改（加）

    // 图片上传根路径（配置为静态资源目录）
    private static final String UPLOAD_DIR = "uploads/news/";

    // 允许的图片格式
    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpg", "image/jpeg", "image/png");

    // 单张图片最大大小（2M）
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    // 初始化上传目录（确保目录存在）
    @PostConstruct
    public void initDir() {
        // 获取项目根目录的绝对路径（兼容Windows/Linux）
        baseDir = System.getProperty("user.dir") + File.separator;
        // 拼接最终的上传目录：项目根目录/uploads/news/
        String uploadDir = baseDir + UPLOAD_DIR;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            // 递归创建多级目录（mkdirs支持创建多层，mkdir只支持单层）
            boolean isCreated = dir.mkdirs();
            System.out.println("图片上传目录创建结果：" + isCreated + "，目录路径：" + uploadDir);
        }
        // 打印最终路径，方便验证
        System.out.println("图片上传根目录：" + baseDir);
        System.out.println("图片保存完整路径：" + uploadDir);
    }




    /**
     * 上传单张图片，返回访问URL
     * 这个方法获取用户上传的一张图片名，生成唯一文件名，保存文件到本地服务器，并且返回新的文件名给调用者，保存到图片数据库
     *
     */
    public String upload(MultipartFile file) throws IOException {
        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("仅支持jpg/png格式的图片！");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("单张图片大小不能超过2M！");
        }

        // 3. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        // 处理文件名可能为null的情况
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("图片文件名不能为空！");
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = System.currentTimeMillis() + "_" + new Random().nextInt(1000, 9999) + suffix;

        // 4. 保存文件（使用绝对路径）
        String filePath = baseDir + UPLOAD_DIR + fileName;
        File destFile = new File(filePath);
        // 确保父目录存在（兜底）
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        file.transferTo(destFile);
        System.out.println("图片保存成功，路径：" + filePath);

        // 5. 返回访问URL（静态资源映射/uploads/** → 对应项目根目录的uploads/）
        return "http://localhost:8080/uploads/news/" + fileName;
    }




    /**
     * 删除本地服务器的图片文件
     */
    public void deleteFile(String imageUrl) {
        System.out.println("要删除的图片的URL:" + imageUrl);
        // 截取文件名：http://localhost:8080/uploads/news/123.jpg → 123.jpg

        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        System.out.println("截取到的文件名:" + fileName);

        String filePath = baseDir + UPLOAD_DIR + fileName;
        System.out.println("要删除的图片完整路径:" + filePath);

        //4.检查文件是否存在
        File file = new File(filePath);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            System.out.println("图片删除结果：" + isDeleted + "，路径：" + filePath);
        } else {
            System.out.println("图片文件不存在，路径：" + filePath);
        }
    }
}
