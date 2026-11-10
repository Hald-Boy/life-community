package com.guat.cloud.content.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsLike {
    private int id;
    private int userID;     //谁点的赞---》唯一约束---》关联用户表
    private int newsID;     //哪条新闻的赞---》唯一约束---》关联新闻表
    private LocalDate createDate;   //点赞的时间
}
