package com.guat.cloud.content.controller;

import com.alibaba.fastjson2.JSON;
import com.guat.cloud.content.annotation.RequiredRole;
import com.guat.cloud.content.dto.*;
import com.guat.cloud.content.entity.*;
import com.guat.cloud.content.service.NewsService;
import com.guat.cloud.content.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 模块标签（Swagger UI 分类）
@Tag(name = "新闻管理接口", description = "提供新闻的增删改查功能，支持新闻图片的上传/修改/删除")
//@SecurityRequirement(name = "BearerAuth")
@Slf4j
@RestController
//@RequestMapping("/api/web/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private HttpServletRequest request; // 直接注入请求对象


    /**
     * 新增新闻（含图片）
     */
    @Operation(
            summary = "发布帖子",
            description = "添加一条新帖子，支持上传多张帖子图片；帖子基本信息为可选，图片为可选，但必须选其中之一。请求格式：Content-Type 为 multipart/form-data，news 字段传 JSON 字符串（如 {\"title\":\"标题\",\"content\":\"内容\"}），newImages 字段传图片文件"
    )
    @Parameters({
            @Parameter(name = "news", description = "帖子标题和内容（JSON格式）", required = false),
            @Parameter(name = "newImages", description = "新闻配图（多张），格式支持jpg/png等，非必填", required = false)
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/api/web/post/add")
    public Result<String> addNews(
            @RequestPart("news") NewsDTO newsDTO,
            @RequestPart(value = "newImages", required = false) MultipartFile[] newImages
    ) {
        log.info("news: {}", newsDTO);
        log.info("图片数量: {}", newImages != null ? newImages.length : 0);

        News news = new News();
        news.setTitle(newsDTO.getTitle());
        news.setContent(newsDTO.getContent());

        try {

            // ===== 获取当前用户ID =====
            Integer userId = UserContext.requireUserId(request);
            if (userId == null) {
                return Result.error("用户未登录或Token无效");
            }

            // 设置新闻的创建者
            news.setUserId(userId);
            log.info("设置userID为: {}", userId);

            // ===== 新增：兼容无图片的情况（避免空指针）=====  2025/12/10  豆包
            MultipartFile[] finalNewImages = newImages == null ? new MultipartFile[0] : newImages;


            // 2026/9/16 豆脑偏方
            // 判断是否存在有效文字：标题 或者 内容不为空（去除空格）
            boolean hasText = (news.getTitle() != null && !news.getTitle().trim().isEmpty())
                    || (news.getContent() != null && !news.getContent().trim().isEmpty());
            // 判断是否上传图片：数组长度大于0
            boolean hasImage = finalNewImages.length > 0;
            // 文字 和 图片同时都没有，拦截
            if (!hasText && !hasImage) {
                return Result.error("不能文字和媒体全空，请填写内容或者上传图片");
            }
            log.info("图片长度：{}", finalNewImages.length);
            log.info("图片:{}", (Object[]) finalNewImages);

            newsService.add(news, finalNewImages);
            return Result.success("新增新闻成功！");

        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增新闻失败！");
        }
    }




    /**
     * 修改帖子（含图片）
     */
    @Operation(
            summary = "修改帖子",
            description = "更新指定ID的帖子信息，支持新增图片、删除旧图片、拖拽排序；帖子ID为必填。请求格式：Content-Type 为 multipart/form-data，news 字段传 JSON 字符串，newImages 传图片文件，keepMediaList/newMediaSortList 传 JSON 数组字符串（也可拼在 URL query 上）"
    )
    @Parameters({
            @Parameter(name = "id", description = "帖子唯一ID", required = true, example = "1"),
            @Parameter(name = "news", description = "更新后的帖子基本信息（JSON格式）", required = true),
            @Parameter(name = "newImages", description = "新增的帖子配图（多张），非必填", required = false),
            @Parameter(name = "keepMediaList", description = "编辑后【要保留】的旧图片列表，JSON数组字符串，元素含id和拖拽后的新sortOrder，如 [{\"id\":15,\"sortOrder\":2}]；不传=不动旧图，传[]=删除全部旧图；可通过URL参数或FormData字段传递", required = false),
            @Parameter(name = "newMediaSortList", description = "新上传图片的排序号JSON数组，与newImages文件一一对应，如 [3,4]；不传则自动追加到已有图片末尾；可通过URL参数或FormData字段传递", required = false)
    })
    @PutMapping("/api/web/post/update/{id}")
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> update(
            @PathVariable Integer id,
            @RequestPart("news") NewsEditDTO newsEditDTO,
            @RequestPart(value = "newImages", required = false) MultipartFile[] newImages,
            // keepMediaList / newMediaSortList 同时兼容两种传法：
            // 1) 放进 FormData 里（@RequestPart 接）；2) 拼在 URL query 上（@RequestParam 接）
            @RequestPart(value = "keepMediaList", required = false) String keepMediaListPart,
            @RequestParam(value = "keepMediaList", required = false) String keepMediaListParam,

            @RequestPart(value = "newMediaSortList", required = false) String newMediaSortListPart,
            @RequestParam(value = "newMediaSortList", required = false) String newMediaSortListParam
    ) {
        log.info("更新帖子 id: {}, news: {}", id, newsEditDTO);
        Integer loginUserId = UserContext.getUserId(request);
        Integer loginRole = UserContext.getRole(request);

        try {
            // 解析 keepMediaList：JSON数组字符串 → List<MediaKeepDTO>，如 [{"id":15,"sortOrder":2}]
            String keepMediaListJson = keepMediaListPart != null ? keepMediaListPart : keepMediaListParam;
            List<MediaKeepDTO> keepMediaList = (keepMediaListJson == null || keepMediaListJson.trim().isEmpty())
                    ? null : JSON.parseArray(keepMediaListJson, MediaKeepDTO.class);
            log.info("保留的图片列表: {}", keepMediaList);

            // 解析 newMediaSortList：JSON数字数组 → List<Integer>，如 [3,4]，与 newImages 文件一一对应
            String newMediaSortListJson = newMediaSortListPart != null ? newMediaSortListPart : newMediaSortListParam;
            List<Integer> newMediaSortList = (newMediaSortListJson == null || newMediaSortListJson.trim().isEmpty())
                    ? null : JSON.parseArray(newMediaSortListJson, Integer.class);
            log.info("新图片排序列表: {}", newMediaSortList);

            newsService.update(id, newsEditDTO, newImages, keepMediaList, newMediaSortList, loginUserId, loginRole);
            return Result.success("修改帖子成功！");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改帖子失败！");
        }
    }




    /**
     * 根据ID查询新闻详情（含图片）
     */
    @Operation(
            summary = "查询帖子详情",
            description = "通过帖子ID获取单条帖子的完整信息，包含关联的图片列表"
    )
    @Parameter(name = "id", description = "帖子唯一ID", required = true, example = "1")
    @GetMapping("/publicApi/web/post/find/{id}")
    public Result<News> getById(@PathVariable Integer id) {
        try {
            News news = newsService.getById(id);
            return Result.success(news);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询新闻失败！");
        }
    }




    /**
     * 删除新闻（含图片）
     */
    @Operation(
            summary = "删除帖子，管理员和帖子作者可用",
            description = "根据帖子ID删除指定帖子（逻辑删除：标记 is_deleted，数据与图片保留）"
    )
    @Parameter(name = "id", description = "帖子唯一ID", required = true, example = "1")
    @DeleteMapping("/api/web/post/delete/{id}")
    @SecurityRequirement(name = "BearerAuth")
    public Result<String> delete(@PathVariable Integer id) {

        Integer logUserId = UserContext.getUserId(request);
        Integer logUserRole = UserContext.getRole(request);
        log.info("（删除帖子）当前登录的用户id为：{}，身份是：{}（0普通用户，1管理员）",logUserId,logUserRole);
        try {
            newsService.delete(id,logUserId,logUserRole);
            return Result.success("删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除新闻失败！");
        }
    }




    /**
     * 分页查询新闻
     */
    @Operation(
            summary = "分页查询帖子",
            description = "支持按帖子标题模糊查询，默认页码1、每页10条数据；仅返回未删除、未封禁的帖子"
    )
    @Parameters({
            @Parameter(name = "title", description = "帖子标题（模糊查询），非必填", required = false, example = "三角洲行动"),
            @Parameter(name = "pageNum", description = "页码，默认值1", required = false, example = "1"),
            @Parameter(name = "pageSize", description = "每页条数，默认值10", required = false, example = "10")
    })
    @GetMapping("/publicApi/web/post/page")
    public Result<PageBean<News>> pageQuery(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        log.info("title: {}, pageNum: {}, pageSize: {}", title, pageNum, pageSize);
        try {
            PageBean<News> pageBean = newsService.pageQuery(title, pageNum, pageSize);
            return Result.success(pageBean);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("分页查询新闻失败！");
        }
    }


    /** 点赞 / 取消点赞（需登录） */
    @Operation(summary = "点赞 / 取消点赞", description = "传入 postId，已赞则取消点赞，未赞则点赞")
    @Parameter(name = "postId", description = "新闻ID", required = true, example = "1")
    @PostMapping("/api/web/post/postlike")
    @SecurityRequirement(name = "BearerAuth")
    public Result<Map<String, Object>> like(@RequestParam Integer postId, HttpServletRequest request) {
        return Result.success(newsService.toggleLike(UserContext.requireUserId(request), postId));
    }

    /** 查询点赞状态（无需登录，未登录视为未点赞） */
    @Operation(summary = "查询点赞状态", description = "返回 {likeCount, isLiked}，未登录按未点赞处理")
    @Parameter(name = "postId", description = "新闻ID", required = true, example = "1")
    @GetMapping("/api/web/post/postlike/status")
    @SecurityRequirement(name = "BearerAuth")
    public Result<Map<String, Object>> likeStatus(@RequestParam Integer postId, HttpServletRequest request) {
        return Result.success(newsService.getLikeStatus(UserContext.getUserId(request), postId));
    }
}
