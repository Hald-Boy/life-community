package com.guat.cloud.user.utils;

public class RedisKeyConstants {


    // ==================== 帖子点赞 ====================
    public static final String LIKE_SET_PREFIX = "post:like:";// 帖子点赞用户集合 Set
    public static final String LIKE_COUNT_PREFIX = "post:like:count:";// 帖子点赞数计数器 String
    public static final String CHANGED_POST_KEY = "post:like:changed";// 帖子点赞变动集合 Set（定时任务同步用）


    // ==================== 评论区 ====================
    public static final String COMMENT_LIKE_SET = "comment:like:";//评论点赞用户集合 Set
    public static final String COMMENT_LIKE_COUNT = "comment:like:count:";//评论点赞数计数器 String
    public static final String COMMENT_LIKE_CHANGED = "comment:like:changed";//评论点赞变动集合 Set
    public static final String COMMENT_REPLY_COUNT = "comment:reply:count:";//单条评论的回复数计数器 String
    public static final String POST_COMMENT_HOT_ZSET = "post:comment:hot:";//帖子热评排行榜 ZSet，score=热度分



}
