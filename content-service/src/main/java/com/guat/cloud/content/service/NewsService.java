package com.guat.cloud.content.service;

import com.guat.cloud.content.dto.MediaKeepDTO;
import com.guat.cloud.content.dto.NewsDTO;
import com.guat.cloud.content.dto.NewsEditDTO;
import com.guat.cloud.content.dto.PageBean;
import com.guat.cloud.content.entity.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface NewsService {

    // 新增新闻（含图片）
    void add(News news, MultipartFile[] newImages) throws IOException;

    // 修改新闻（含图片新增/删除）
    void update(Integer id, NewsEditDTO newsEditDTO, MultipartFile[] newImages, List<MediaKeepDTO> keepMediaList, List<Integer> newMediaSortList, Integer loginUserId, Integer loginUserRole) throws IOException;

    // 根据ID查询新闻（含图片）
    News getById(Integer id);

    // 删除新闻（含图片）
    void delete(Integer id, Integer loginUserId, Integer loginUserRole) throws IOException;

    // 分页查询新闻
    PageBean<News> pageQuery(String title, Integer pageNum, Integer pageSize);

    /** 点赞/取消点赞帖子，返回 {likeCount, isLiked} */
    Map<String, Object> toggleLike(Integer userId, Integer postId);

    /** 查询点赞状态，userId 为空时按未点赞处理，返回 {likeCount, isLiked} */
    Map<String, Object> getLikeStatus(Integer userId, Integer postId);

}