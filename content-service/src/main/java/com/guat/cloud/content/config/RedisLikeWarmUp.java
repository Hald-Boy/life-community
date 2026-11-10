package com.guat.cloud.content.config;

import com.guat.cloud.content.entity.Comment;
import com.guat.cloud.content.entity.CommentLike;
import com.guat.cloud.content.entity.News;
import com.guat.cloud.content.entity.PostLike;
import com.guat.cloud.content.mapper.CommentLikeMapper;
import com.guat.cloud.content.mapper.CommentMapper;
import com.guat.cloud.content.mapper.NewsMapper;
import com.guat.cloud.content.mapper.PostLikeMapper;
import com.guat.cloud.content.utils.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis 预热：启动时把 DB 里的点赞数据 + 评论热评榜灌进 Redis。
 * Redis 重启后数据丢失，靠这个 Runner 重建。
 */
@Component
public class RedisLikeWarmUp implements ApplicationRunner {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private CommentLikeMapper commentLikeMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private NewsMapper newsMapper;

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("========== 开始预热 Redis 点赞数据 ==========");
        warmUpPostLikes();
        warmUpCommentLikes();
        warmUpHotCommentZSet();
        System.out.println("========== Redis 点赞数据预热完成 ==========");
    }

    /** 帖子点赞：DB → Redis Set + count */
    private void warmUpPostLikes() {
        List<PostLike> allValidLikes = postLikeMapper.selectAllValidLikes();
        if (allValidLikes == null || allValidLikes.isEmpty()) {
            return;
        }
        allValidLikes.stream()
                .collect(Collectors.groupingBy(PostLike::getNewsId))
                .forEach((postId, likeList) -> {
                    String setKey = RedisKeyConstants.LIKE_SET_PREFIX + postId;
                    String countKey = RedisKeyConstants.LIKE_COUNT_PREFIX + postId;
                    Object[] userIds = likeList.stream().map(PostLike::getUserId).toArray();
                    redisTemplate.opsForSet().add(setKey, userIds);
                    redisTemplate.opsForValue().set(countKey, (long) likeList.size());
                });
        System.out.println("  [帖子点赞] 共预热 " + allValidLikes.size() + " 条");
    }

    /** 评论点赞：DB → Redis Set + count */
    private void warmUpCommentLikes() {
        List<CommentLike> allValid = commentLikeMapper.selectAllValidLikes();
        if (allValid == null || allValid.isEmpty()) {
            return;
        }
        allValid.stream()
                .collect(Collectors.groupingBy(CommentLike::getCommentId))
                .forEach((commentId, likeList) -> {
                    String setKey = RedisKeyConstants.COMMENT_LIKE_SET + commentId;
                    String countKey = RedisKeyConstants.COMMENT_LIKE_COUNT + commentId;
                    Object[] userIds = likeList.stream().map(CommentLike::getUserId).toArray();
                    redisTemplate.opsForSet().add(setKey, userIds);
                    redisTemplate.opsForValue().set(countKey, (long) likeList.size());
                });
        System.out.println("  [评论点赞] 共预热 " + allValid.size() + " 条");
    }

    /** 热评榜：按根评论的历史数据重算 score，灌进 ZSet（post:comment:hot:{postId}） */
    private void warmUpHotCommentZSet() {
        List<Comment> rootComments = commentMapper.listAllRootCommentsForWarmUp();
        if (rootComments == null || rootComments.isEmpty()) {
            return;
        }
        int count = 0;
        for (Comment c : rootComments) {
            long likeCount = c.getLikeCount() == null ? 0 : c.getLikeCount();
            long replyCount = c.getChildren() == null ? 0 : c.getChildren();

            // base = 赞×3 + 回复×1（预热不查踩数，量极小，后续 toggleDislike 会重算）
            double base = likeCount * 3 + replyCount * 1;

            // 楼主评论额外 +5
            News post = newsMapper.findById(c.getNewsId().intValue());
            if (post != null && c.getUserId() != null
                    && c.getUserId().longValue() == post.getUserId().longValue()) {
                base += 5;
            }

            // 时间衰减
            long days = Duration.between(c.getCreateTime(), LocalDateTime.now()).toDays();
            if (days < 0) days = 0;
            double score = base / (1 + days * 0.5);

            redisTemplate.opsForZSet().add(
                    RedisKeyConstants.POST_COMMENT_HOT_ZSET + c.getNewsId(), c.getId(), score);

            // 回复数计数器也灌进去，运行时 incr 才有基准
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.COMMENT_REPLY_COUNT + c.getId(), replyCount);
            count++;
        }
        System.out.println("  [热评榜] 共重建 " + count + " 条根评论");
    }
}
