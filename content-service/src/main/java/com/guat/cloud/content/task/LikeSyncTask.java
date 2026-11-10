package com.guat.cloud.content.task;

import com.guat.cloud.content.entity.News;
import com.guat.cloud.content.entity.PostLike;
import com.guat.cloud.content.mapper.NewsMapper;
import com.guat.cloud.content.mapper.PostLikeMapper;
import com.guat.cloud.content.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定时任务
 */
@Component
public class LikeSyncTask {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private UserMapper userMapper;

    private static final String LIKE_SET_PREFIX = "post:like:";
    private static final String LIKE_COUNT_PREFIX = "post:like:count:";
    private static final String CHANGED_POST_KEY = "post:like:changed"; //只同步有改动的帖子，不用全量遍历所有帖子，减少数据库 IO

    /**
     * 每60秒执行一次，增量同步点赞数据
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void syncLikeToMySQL() {
        System.out.println("【定时任务】执行点赞同步，时间：" + System.currentTimeMillis());
        // 1. 获取所有有变动的帖子ID
        Set<Object> changedPosts = redisTemplate.opsForSet().members(CHANGED_POST_KEY);
        if (changedPosts == null || changedPosts.isEmpty()) {
            return;
        }

        // 收集本次涉及的作者ID，去重，避免同一个作者多次重算
        Set<Long> authorIdSet = new HashSet<>();

        // 2. 遍历每个变动的帖子，逐个同步点赞记录
        for (Object postObj : changedPosts) {
            Integer postId = Integer.valueOf(postObj.toString());
            Long authorId = syncSinglePost(postId);
            if (authorId != null) {
                authorIdSet.add(authorId);
            }
        }

        // 3. 重算所有涉及作者的总获赞数（绝对准确，覆盖式更新）
        // 不会因为多次同步、网络异常产生累计误差，保证数据绝对准确。
        for (Long authorId : authorIdSet) {
            recalculateUserTotalLike(authorId);
        }

        // 4. 同步完成，清空变动集合
        redisTemplate.delete(CHANGED_POST_KEY);
    }


    /**
     * 同步单个帖子的点赞数据
     * @param postId 帖子id
     * @return 返回作者id
     */
    private Long syncSinglePost(Integer postId) {
        String setKey = LIKE_SET_PREFIX + postId;

        // 1. 获取Redis中当前点赞的所有用户ID
        Set<Object> redisLikeUsers = redisTemplate.opsForSet().members(setKey);
        Set<Long> redisUidSet = (redisLikeUsers == null) ? Set.of() :
                redisLikeUsers.stream()
                        .map(o -> Long.valueOf(o.toString()))
                        .collect(Collectors.toSet());

        // 2. 获取MySQL中该帖子的所有点赞记录
        List<PostLike> dbLikeList = postLikeMapper.selectByPostId(postId.longValue());
        // 筛选出MySQL中当前有效（未取消）的用户ID
        Set<Long> dbValidUidSet = dbLikeList.stream()
                .filter(like -> like.getIsCancel() == 0)
                .map(PostLike::getUserId)
                .collect(Collectors.toSet());

        // 3. 找出：Redis有、MySQL没有 → 新增点赞
        for (Long uid : redisUidSet) {
            // 判断：数据库合法uid集合，**不包含这个uid**
            // 含义：这个uid是Redis新同步过来的、数据库里还没标记有效的点赞用户
            if (!dbValidUidSet.contains(uid)) {
                // 根据用户ID+帖子ID，去点赞表查询这条点赞的历史记录
                PostLike history = postLikeMapper.findByUserAndPost(uid, postId.longValue());

                if (history == null) {
                    // 情况1：数据库完全没有这条点赞记录 → 用户第一次点赞这篇帖子
                    PostLike pl = new PostLike();
                    pl.setUserId(uid);
                    pl.setNewsId(postId.longValue());
                    pl.setIsCancel(0); // isCancel=0：未取消，有效点赞
                    postLikeMapper.insert(pl); // 新增点赞数据入库
                } else {
                    // 情况2：查到历史记录！说明用户【之前点赞，后来取消了】
                    // 把isCancel更新为0，恢复点赞状态
                    postLikeMapper.updateCancel(history.getId(), 0);
                }
            }
        }

        // 4. 找出：MySQL有效、Redis没有 → 取消点赞
        // 如果数据库标记这条点赞是有效的，但是 Redis 里面已经找不到这个用户了，代表用户已经在前端点了取消点赞，需要同步更新数据库，把这条点赞改成【已取消】。
        for (PostLike dbLike : dbLikeList) {
            if (dbLike.getIsCancel() == 0 && !redisUidSet.contains(dbLike.getUserId())) {
                postLikeMapper.updateCancel(dbLike.getId(), 1);
            }
        }

        // 5. 更新帖子表的点赞总数（以Redis为准：覆盖成当前集合大小，不能累加）
        long count = redisUidSet.size();
        newsMapper.setLikeCount(postId, (int) count);

        // 6. 返回帖子作者ID，后面统一重算总获赞
        News post = newsMapper.findById(postId);
        return post == null ? null : Long.valueOf(post.getUserId());
    }

    /**
     * 重算指定用户的总获赞数：直接用SQL求和，覆盖更新
     * 绝对准确，无累积误差
     */
    private void recalculateUserTotalLike(Long userId) {
        // 查询该用户所有有效帖子的点赞数之和
        Integer totalLike = newsMapper.sumLikeCountByUserId(userId);
        if (totalLike == null) {
            totalLike = 0;
        }
        // 直接覆盖更新，不是加减
        userMapper.setTotalLikeCount(userId, totalLike);
    }
}

