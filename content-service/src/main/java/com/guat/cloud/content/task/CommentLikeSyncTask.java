package com.guat.cloud.content.task;

import com.guat.cloud.content.entity.CommentLike;
import com.guat.cloud.content.mapper.CommentLikeMapper;
import com.guat.cloud.content.mapper.CommentMapper;
import com.guat.cloud.content.utils.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论点赞定时同步任务（与帖子点赞 LikeSyncTask 对称）：
 * Redis 是实时权威，MySQL 是异步落地。每 60 秒把 Redis 里有变动的评论点赞 diff 到 DB。
 */
@Component
public class CommentLikeSyncTask {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private CommentLikeMapper commentLikeMapper;
    @Autowired
    private CommentMapper commentMapper;

    /**
     * 每 60 秒执行一次，增量同步有变动的评论点赞
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void syncCommentLikeToMySQL() {
        // 1. 拿到本次有变动的评论 id 集合
        Set<Object> changedComments = redisTemplate.opsForSet().members(RedisKeyConstants.COMMENT_LIKE_CHANGED);
        if (changedComments == null || changedComments.isEmpty()) {
            return;
        }
        System.out.println("【评论点赞同步】本次待同步评论数：" + changedComments.size());

        // 2. 逐个 diff
        for (Object cObj : changedComments) {
            Long commentId = Long.valueOf(cObj.toString());
            syncSingleComment(commentId);
        }

        // 3. 同步完成，清空变动集合
        redisTemplate.delete(RedisKeyConstants.COMMENT_LIKE_CHANGED);
    }

    /**
     * 同步单条评论的点赞数据：
     * Redis Set 是当前"谁有效赞了"的权威状态，DB 跟它对齐。
     */
    private void syncSingleComment(Long commentId) {
        String setKey = RedisKeyConstants.COMMENT_LIKE_SET + commentId;

        // Redis 当前有效点赞用户
        Set<Object> redisLikeUsers = redisTemplate.opsForSet().members(setKey);
        Set<Long> redisUidSet = (redisLikeUsers == null) ? Set.of() :
                redisLikeUsers.stream()
                        .map(o -> Long.valueOf(o.toString()))
                        .collect(Collectors.toSet());

        // DB 该评论的全部点赞历史
        List<CommentLike> dbList = commentLikeMapper.selectByCommentId(commentId);
        Set<Long> dbValidUidSet = dbList.stream()
                .filter(l -> Integer.valueOf(0).equals(l.getIsCancel()))
                .map(CommentLike::getUserId)
                .collect(Collectors.toSet());

        // Redis 有、DB 没效 → 新增点赞（新记录 insert 或恢复历史记录 is_cancel=0）
        for (Long uid : redisUidSet) {
            if (!dbValidUidSet.contains(uid)) {
                CommentLike history = commentLikeMapper.findByUserAndComment(uid, commentId);
                if (history == null) {
                    CommentLike cl = new CommentLike();
                    cl.setUserId(uid);
                    cl.setCommentId(commentId);
                    cl.setIsCancel(0);
                    commentLikeMapper.insert(cl);
                } else {
                    commentLikeMapper.updateCancel(history.getId(), 0);
                }
            }
        }

        // DB 有效、Redis 没了 → 取消点赞（is_cancel=1）
        for (CommentLike dbLike : dbList) {
            if (Integer.valueOf(0).equals(dbLike.getIsCancel()) && !redisUidSet.contains(dbLike.getUserId())) {
                commentLikeMapper.updateCancel(dbLike.getId(), 1);
            }
        }

        // 覆盖式把评论点赞总数写成 Redis 集合大小（不能累加！）
        commentMapper.setLikeCount(commentId, redisUidSet.size());
    }
}
