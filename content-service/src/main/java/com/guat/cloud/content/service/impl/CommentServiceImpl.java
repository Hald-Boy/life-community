package com.guat.cloud.content.service.impl;

import com.guat.cloud.content.entity.Comment;
import com.guat.cloud.content.entity.News;
import com.guat.cloud.content.dto.PageBean;
import com.guat.cloud.content.exception.BusinessException;
import com.guat.cloud.content.mapper.CommentDislikeMapper;
import com.guat.cloud.content.mapper.CommentMapper;
import com.guat.cloud.content.mapper.NewsMapper;
import com.guat.cloud.content.service.CommentService;
import com.guat.cloud.content.entity.CommentDislike;
import com.guat.cloud.content.utils.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentDislikeMapper commentDislikeMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据帖子ID去查询一级评论（默认按热评分倒序，抖音式，不提供"最新"切换）
     * 说白了就是先查 redis 带有 id 和分数的集合，然后去掉分数，再拿我们得到的 id 去查数据库里的评论，
     * 此时查到的评论热度顺序是不保证的，所以最后要从数据库里的评论一个个对比 redis 里已经排好的顺序，
     * 对比到了就加入到 list 集合返回，没对比到就说明落库前被删了
     * @param id            帖子ID
     * @param currentUserId 当前登录用户ID（可为 null，游客不附带点赞状态）
     * @return 封装一级评论和条数
     */
    @Override
    public PageBean<Comment> getCommentByID(Integer id, int pageNum, int pageSize, Integer currentUserId) {

        if (pageNum < 1) pageNum = 1;
        int offset = (pageNum - 1) * pageSize; // 0

        List<Comment> list;

        // 1. 从热评 ZSet 里按 score 倒序取当前页的评论 id
        String hotKey = RedisKeyConstants.POST_COMMENT_HOT_ZSET + id; // 评论热度key
        // 整体是一个有序集合，顺序已经按分数从高到低排好了
        // 里面每个元素都是 TypedTuple，可以理解成一个 “数据对”：
        // tuple.getValue() → 拿到成员（评论 ID）
        // tuple.getScore() → 拿到对应的分数（热度分）
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(hotKey, offset, offset + pageSize - 1);

        if (tuples == null || tuples.isEmpty()) {
            // ZSet 无数据（预热前/无评论）→ 兜底走数据库时间倒序
            list = commentMapper.getCommentByNewsId(id, offset, pageSize, currentUserId);
        } else {
            // 2. 按 ZSet 顺序拿到评论 id 列表
            List<Long> hotIds = tuples.stream()
                    .map(t -> Long.valueOf(t.getValue().toString()))
                    .collect(Collectors.toList());

            // 3. 回表查评论详情（SQL IN 查询不保证顺序，下面再按 ZSet 顺序重排）
            List<Comment> dbList = commentMapper.listByIds(hotIds, currentUserId);
            Map<Long, Comment> dbMap = dbList.stream()
                    .collect(Collectors.toMap(Comment::getId, c -> c));

            // 4. 按 ZSet 分数顺序重排；已删除的评论（dbMap 查不到）自动跳过
            list = new ArrayList<>();
            for (Long cid : hotIds) {
                Comment c = dbMap.get(cid);
                if (c != null) {
                    list.add(c);
                }
            }
        }

        // 5. 点赞状态以 Redis 为准（窗口期内 DB 还没同步，不能信 DB）
        applyRedisLiked(list, currentUserId);

        // 获取一级评论的总条数
        Long total = commentMapper.countRootComment(id);
        long safeTotal = total == null ? 0 : total;
        return new PageBean<>(list, safeTotal, pageNum, pageSize);
    }



    /**
     * 根据帖子ID和父评论ID查找父评论的子评论（楼中楼仍按时间正序，不参与热评）
     */
    @Override
    public PageBean<Comment> getChildComment(Integer newsId, Integer parentId, int pageNum, int pageSize, Integer currentUserId) {

        if (pageNum < 1) pageNum = 1;
        int offset = (pageNum - 1) * pageSize;

        //根据newsId和parentId查询所有的子评论
        List<Comment> list = commentMapper.getChildComment(newsId, parentId, offset, pageSize, currentUserId);

        // 点赞状态同样以 Redis 为准
        applyRedisLiked(list, currentUserId);

        //获取子评论的条数
        Long total = commentMapper.countChildComment(newsId, parentId);
        long safeTotal = total == null ? 0 : total;

        return new PageBean<>(list, safeTotal, pageNum, pageSize);
    }


    /**
     * 判断用户是否点赞
     * 用 Redis 实时点赞集合覆盖评论的 liked 字段。
     * 评论点赞是实时写 Redis、60 秒定时任务才落库，窗口期内 DB 查不到，必须以 Redis 为准。
     */
    private void applyRedisLiked(List<Comment> list, Integer currentUserId) {
        if (currentUserId == null || list == null || list.isEmpty()) {
            return;
        }
        Long uid = currentUserId.longValue();
        for (Comment c : list) {
            Boolean member = redisTemplate.opsForSet()
                    .isMember(RedisKeyConstants.COMMENT_LIKE_SET + c.getId(), uid);
            c.setLiked(Boolean.TRUE.equals(member));
        }
    }


    /**
     * 新增评论
     * @param comment 评论的信息（"帖子newsId", "父parentId", "回复谁toUserId", "内容comment")
     * @return 受影响条数 >0 代表插入成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean addComment(Comment comment) {

        comment.setStatus(1); //设置评论有效
        comment.setCreateTime(LocalDateTime.now()); //设置评论的时间

        // 情况1：一级评论（不回复任何人）
        if (comment.getParentId() == null || comment.getParentId() == 0L) {
            comment.setParentId(0L);
            comment.setRootCommentId(null);
        } else {
            // 情况2：非一级评论，回复别人（楼中楼）
            Comment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment == null) {
                return false;
            }
            // 父是一级评论 → 当前评论的root就是父的id
            if (parentComment.getRootCommentId() == null) {
                comment.setRootCommentId(parentComment.getId());
            } else {
                // 父本身也是子评论 → 继承父的 rootId
                comment.setRootCommentId(parentComment.getRootCommentId());
            }
        }
        int total = commentMapper.insertComment(comment);
        // 帖子评论数+1
        newsMapper.updateCommentCount(comment.getNewsId(), 1);

        // ===== 热评 ZSet 维护 =====
        Long newId = comment.getId();
        if (comment.getParentId() == 0L) {
            // 新一级评论：加入热评榜（初始分数由 recalcHotScore 按公式算，新评论从 0 起步往上爬）
            recalcHotScore(newId);
        } else if (comment.getRootCommentId() != null) {
            // 子评论：根评论回复数+1，并重算根评论分数（回复会提升父评论热度）
            redisTemplate.opsForValue().increment(
                    RedisKeyConstants.COMMENT_REPLY_COUNT + comment.getRootCommentId(), 1);
            recalcHotScore(comment.getRootCommentId());
        }

        return total > 0;
    }

    /**
     * 删除评论
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean delComment(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        int total = commentMapper.deleteComment(id, userId);
        if (comment != null) {
            // 帖子评论数-1
            newsMapper.updateCommentCount(comment.getNewsId(), -1);
            // 从热评 ZSet 移除
            redisTemplate.opsForZSet().remove(
                    RedisKeyConstants.POST_COMMENT_HOT_ZSET + comment.getNewsId(), id);
        }
        return total > 0;
    }



    /**
     * 评论点赞/取消点赞（实时写 Redis，DB 由定时任务异步同步）
     * @return { isLiked: true=刚赞上/false=刚取消, likeCount: 当前评论点赞总数 }
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Integer userId, Long commentId) {

        String setKey = RedisKeyConstants.COMMENT_LIKE_SET + commentId;
        String countKey = RedisKeyConstants.COMMENT_LIKE_COUNT + commentId;
        Long uid = userId.longValue();

        SetOperations<String, Object> setOps = redisTemplate.opsForSet();

        // 1.判断当前用户是否已经点赞
        Boolean isLike = setOps.isMember(setKey, uid);
        boolean currentLiked = Boolean.TRUE.equals(isLike);

        if (!currentLiked) {
            // 未点赞 → 点赞
            setOps.add(setKey, uid);
            redisTemplate.opsForValue().increment(countKey, 1);
        } else {
            // 已点赞 → 取消
            setOps.remove(setKey, uid);
            redisTemplate.opsForValue().increment(countKey, -1);
        }

        // 2.标记这条评论有变动，等待定时任务落库（用评论自己的 changed 集合）
        setOps.add(RedisKeyConstants.COMMENT_LIKE_CHANGED, commentId);

        // 3.当前点赞数以 Redis Set 大小为准
        Long likeCount = setOps.size(setKey);

        // 4.点赞状态变化会影响热评分，重算该评论在 ZSet 里的分数
        recalcHotScore(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", Math.max(likeCount == null ? 0 : likeCount, 0));
        result.put("isLiked", !currentLiked);
        return result;
    }


    /**
     * 重算单条评论的热评分并写回 ZSet。
     * 公式（加权求和 + 时间衰减）：
     *   base  = 点赞数×3 + 回复数×1 − 踩数×2 + (楼主发的评论额外 +5)
     *   score = base / (1 + 创建至今天数 × 0.5)   // 越新的评论越有机会上位
     */
    private void recalcHotScore(Long commentId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) {
            return;
        }
        String hotKey = RedisKeyConstants.POST_COMMENT_HOT_ZSET + c.getNewsId();

        // 评论已下架 → 从热评榜移除
        if (c.getStatus() == null || c.getStatus() != 1) {
            redisTemplate.opsForZSet().remove(hotKey, commentId);
            return;
        }

        // 点赞数：优先 Redis 实时集合大小，Redis 无集合时兜底 DB
        Long likeSize = redisTemplate.opsForSet().size(RedisKeyConstants.COMMENT_LIKE_SET + commentId);
        long likeCount = (likeSize == null || likeSize == 0)
                ? (c.getLikeCount() == null ? 0 : c.getLikeCount())
                : likeSize;

        // 回复数：Redis 计数器（addComment 时 incr），无则 0
        Object replyObj = redisTemplate.opsForValue().get(RedisKeyConstants.COMMENT_REPLY_COUNT + commentId);
        long replyCount = replyObj == null ? 0 : Long.parseLong(replyObj.toString());

        // 踩数：DB comment_dislike 有效记录数（踩量小，没接 Redis，直接查）
        int dislikeCount = commentDislikeMapper.countValidByCommentId(commentId);

        // 加权求和 double base = likeCount * 3 + replyCount * 1 - dislikeCount * 2;
        double base = likeCount * 3 + replyCount - dislikeCount * 2L;

        // 楼主（帖子作者本人）发的评论额外 +5，相当于官方发言位
        News post = newsMapper.findById(c.getNewsId().intValue());
        if (post != null && c.getUserId() != null
                && c.getUserId() == post.getUserId().longValue()) {
            base += 5;
        }

        // 时间衰减：每过一天打一次折
        long days = Duration.between(c.getCreateTime(), LocalDateTime.now()).toDays();
        if (days < 0) days = 0;
        double score = base / (1 + days * 0.5);

        redisTemplate.opsForZSet().add(hotKey, commentId, score);
    }


    /**
     * 评论折叠（踩）功能
     * @return { isDisliked: true=已踩/false=已取消 }
     */
    @Override
    public Map<String, Object> toggleDislike(Integer userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        boolean isDisliked;
        CommentDislike record = commentDislikeMapper.findByUserAndComment(userId.longValue(), commentId);
        if (record == null) {
            CommentDislike cd = new CommentDislike();
            cd.setUserId(userId.longValue());
            cd.setCommentId(commentId);
            cd.setIsCancel(0);
            commentDislikeMapper.insert(cd);
            isDisliked = true;
        } else if (Integer.valueOf(1).equals(record.getIsCancel())) {
            commentDislikeMapper.updateCancel(record.getId(), 0);
            isDisliked = true;
        } else {
            commentDislikeMapper.updateCancel(record.getId(), 1);
            isDisliked = false;
        }

        // 踩数变化会影响热评分，重算
        recalcHotScore(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isDisliked", isDisliked);
        return result;
    }

    /**
     * 查询当前用户已折叠的评论 id 集合（评论列表初始化时用）
     */
    @Override
    public List<Long> getFoldedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return commentDislikeMapper.findFoldedCommentIds(userId, commentIds);
    }
}
