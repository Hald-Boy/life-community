package com.guat.cloud.report.mapper;

import com.guat.cloud.report.dto.PostCardVO;
import com.guat.cloud.report.entity.News;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NewsMapper {

    //2025/12/5   ------AI
    // 新增新闻（返回自增ID）
    int insert(News news);

    // 修改新闻
    int update(News news);

    // 删除新闻
    /** 逻辑删除：标记 is_deleted='1'，保留数据与文件 */
    int logicalDelete(@Param("id") Integer id);

    // 根据ID查询新闻
    News selectById(Integer id);

    // 分页查询新闻（支持模糊标题查询）
    List<News> selectByPage(@Param("title") String title, @Param("start") Integer start, @Param("pageSize") Integer pageSize);

    // 分页查询总记录数
    Long selectTotal(@Param("title") String title);

    // 根据id查询帖子，和上面的 selectById() 是一样的功能，后续优化记得只留其一
    News findById(@Param("id") Integer id);

    /** 增减帖子点赞数，delta 可为 ±1 */
    int updateLikeCount(@Param("id") Integer id, @Param("delta") int delta);

    /** 覆盖式设置帖子点赞总数（Redis 同步用，以 Redis 实际集合大小为准，不做累加） */
    int setLikeCount(@Param("id") Integer id, @Param("likeCount") int likeCount);

    /** 增减帖子收藏数，delta 可为 ±1 */
    // 2026/9/7  P1新增
    int updateCollectCount(@Param("id") Integer id, @Param("delta") int delta);

    /**增减评论总数*/
    int updateCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /** 某用户的帖子数（我的作品） */
    int countByUserId(@Param("userId") Integer userId);

    /** 某用户的帖子分页列表（我的作品，带首图） */
    List<PostCardVO> listByUserId(@Param("userId") Integer userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /** 更新帖子状态：status 0 正常 1 下架(封禁) */
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 统计指定用户所有有效帖子的点赞数之和
     */
    Integer sumLikeCountByUserId(Long userId);
}