package com.guat.cloud.content.mapper;

import com.guat.cloud.content.entity.NewsImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface NewsImageMapper {
    // 批量插入新闻图片
    int batchInsert(@Param("images") List<NewsImage> images);

    // 根据新闻ID查询图片
    List<NewsImage> selectByNewsId(Integer newsId);

    // 根据图片ID删除
    int deleteByIds(@Param("ids") List<Integer> ids);

    // 根据新闻ID删除图片
    int deleteByNewsId(Integer newsId);

    // 逻辑删除图片（按ID集合，标记 is_deleted 为 1，保留数据与文件）
    int logicalDeleteByIds(@Param("ids") List<Integer> ids);

    int updateById(NewsImage newsImage);
}
