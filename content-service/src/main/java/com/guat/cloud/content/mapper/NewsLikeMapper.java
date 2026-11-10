package com.guat.cloud.content.mapper;

import com.guat.cloud.content.entity.NewsLike;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewsLikeMapper {
    @Insert("insert into news_like (user_id, news_id, create_time) values (#{userID}, #{newsID}, now())")
    void insertNewsLike(NewsLike newsLike);

    @Delete("delete from news_like where user_id = #{userID} and news_id = #{newsID}")
    void deleteNewsLike(NewsLike newsLike);
}
