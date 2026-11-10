package com.guat.cloud.user.service.impl;

import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.UserCardVO;
import com.guat.cloud.user.dto.UserInfo;
import com.guat.cloud.user.entity.User;
import com.guat.cloud.user.entity.UserFollow;
import com.guat.cloud.user.exception.BusinessException;
import com.guat.cloud.user.mapper.UserFollowMapper;
import com.guat.cloud.user.mapper.UserMapper;
import com.guat.cloud.user.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 关注/取消关注
     *
     * @param userId 当前登录用户的id
     * @param targetUserId 当前用户关注者的id
     * @return 关注返回true取消关注返回false
     * {
     *  "code": 200,
     *  "msg": "success",
     *  "data": {
     *      "isFollowing": true
     *      }
     * }
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFollow(Integer userId, Integer targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能关注自己");
        }
        // 根据当前用户关注者的id，查询这个被关注者的信息
        User target = userMapper.findById(targetUserId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        int delta;
        // 查询彼此是否存在关注记录
        UserFollow record = userFollowMapper.findByUserAndFollowUser(userId.longValue(), targetUserId.longValue());
        if (record == null) {
            UserFollow uf = new UserFollow();
            // 设置关注者的id为当前登录用户的id
            uf.setUserId(userId.longValue());
            // 设置被关注者的id
            uf.setFollowUserId(targetUserId.longValue());
            // 设置为已关注（有效关注） 0有效 1无效（取消关注）
            uf.setIsCancel(0);
            // 把数据插入“用户关注表”
            userFollowMapper.insert(uf);
            // delta设置为1，为被关注者的总关注数 +1
            delta = 1;
        } else if (Integer.valueOf(1).equals(record.getIsCancel())) {
            // 如果已经存在关注记录，但isCancel ！= 1，说明已取消关注
            // 之前已取消 -> 本次操作为：重新关注，设置isCancel为 0，旧isCancel为 1
            // 并发情况： 2026/9/4 设计
            // 就是线程A和线程B都读到isCancel=1，（我们设置的旧状态是1），线程A比线程B稍快，线程A在线程B更新isCancel之前就已经把isCancel更新成了0，也就是先执行
            // UPDATE user_follow SET is_cancel = 0 ,update_time = NOW() WHERE id = 100 AND is_cancel = 1,条件满足，此刻数据库的isCancel真实数据变成了0，受影响条数为1
            // 线程B就会执行 UPDATE user_follow SET is_cancel = 0 ,update_time = NOW() WHERE id = 100 AND is_cancel = 1,此时真实的is_cancel=0条件不满足，受影响条数为0
            // 如果线程B是执行UPDATE user_follow SET is_cancel = 0 ,update_time = NOW() WHERE id = 100,条件依旧满足，会继续往下走执行delta = 1进行修改user表的数据，这是我们不希望看到的
            int affectRow = userFollowMapper.updateCancel(record.getId(), 0,1);
            // 如果受影响行数0，代表并发下状态已经被别人改动
            if (affectRow == 0) {
                // 并发冲突，重新查询最新真实状态
                UserFollow latest = userFollowMapper.findByUserAndFollowUser(userId.longValue(), targetUserId.longValue());
                boolean realIsFollow = latest != null && Integer.valueOf(0).equals(latest.getIsCancel());

                Map<String,Object> result = new HashMap<>();
                result.put("isFollowing", realIsFollow);
                return result;
            }
            delta = 1;
        } else {
            // 已关注 -> 本次操作为：取消关注
            // 并发情况：
            // 就是线程A和线程B都读到isCancel=0，（我们设置的旧状态是0），线程A比线程B稍快，线程A在线程B更新isCancel之前就已经把isCancel更新成了1，也就是先执行
            // UPDATE user_follow SET is_cancel = 1 ,update_time = NOW() WHERE id = 100 AND is_cancel = 0,条件满足，此刻数据库的isCancel真实数据变成了1，受影响条数为1
            // 线程B就会执行 UPDATE user_follow SET is_cancel = 1 ,update_time = NOW() WHERE id = 100 AND is_cancel = 0,此时条件不满足，受影响条数为0
            // 如果线程B是执行UPDATE user_follow SET is_cancel = 1 ,update_time = NOW() WHERE id = 100,条件依旧满足，会继续往下走执行delta = -1进行修改user表的数据，这是我们不希望看到的
            int  affectRow = userFollowMapper.updateCancel(record.getId(), 1,0);
            if (affectRow == 0) {

                // 并发冲突，重新查询最新真实状态
                UserFollow latest = userFollowMapper.findByUserAndFollowUser(userId.longValue(), targetUserId.longValue());
                boolean realIsFollow = latest != null && Integer.valueOf(0).equals(latest.getIsCancel());
                Map<String,Object> result = new HashMap<>();
                result.put("isFollowing", realIsFollow);
                return result;
            }
            // 被关注者总关注数 -1
            delta = -1;
        }
        // 同步双方冗余计数：关注者的 follow_count + 被关注人的 fan_count
        // 更新当前用户的关注数
        userMapper.updateFollowCount(userId, delta);
        // 更新目标用户的被关注数（粉丝数）
        userMapper.updateFanCount(targetUserId, delta);
        // 封装结果返回
        Map<String, Object> result = new HashMap<>();
        result.put("isFollowing", delta > 0);
        return result;
    }


    /**
     * 关注状态，viewerId 为空按未登录处理，返回 {isFollowing, isMutual}
     * 场景               ifFollowing         isMutual
     * 未登录              false               false
     * 已登录未关注         false               false
     * 已关注，对方没关注    true                false
     * 相互关注            true                true
     * @param viewerId 当前登录用户id，游客为空
     * @param targetUserId 目标用户id
     * @return 返回关注状态
     * 2026/9/3 --hzw
     *
     */
    @Override
    public Map<String, Object> getFollowStatus(Integer viewerId, Integer targetUserId) {
        // 根据目标用户的id，查询这个被关注者的信息
        User target = userMapper.findById(targetUserId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        boolean isFollowing = false;// 是否关注
        boolean isMutual = false;// 是否相互关注
        // 当前登录用户id不为空，即已登录的用户
        if (viewerId != null) {
            // 查询viewerId是否关注了targetUserId
            UserFollow forward = userFollowMapper.findByUserAndFollowUser(viewerId.longValue(), targetUserId.longValue());
            // 如果viewerId关注了targetUserId，关注状态也为0有效，isFollowing则为true，反之则为false
            isFollowing = forward != null && Integer.valueOf(0).equals(forward.getIsCancel());
            // viewerId关注了targetUserId，才需要查询targetUserId是否关注了viewerId
            if (isFollowing) {
                // 查询targetUserId是否关注了viewerId
                UserFollow reverse = userFollowMapper.findByUserAndFollowUser(targetUserId.longValue(), viewerId.longValue());
                isMutual = reverse != null && Integer.valueOf(0).equals(reverse.getIsCancel());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("isMutual", isMutual);
        return result;
    }


    /**
     * 关注列表
     * 可在“设置”设置为他人可见/不可见
     * @param viewerId 查看关注列表者的id
     * @param userId 被查看关注列表者的id
     * @param pageNum 页码
     * @param pageSize 一页记录数
     * @return 返回关注列表用户卡片，条数，页码，一页记录数
     */
    @Override
    public PageBean<UserCardVO> listFollowing(Integer viewerId, Integer userId, int pageNum, int pageSize) {
        // 查询目标用户的关注总数
        long total = userFollowMapper.countFollowing(userId.longValue());
        // 查询目标用户的关注列表
        List<User> users = userFollowMapper.listFollowing(userId.longValue(), (pageNum - 1) * pageSize, pageSize);
        return new PageBean<>(buildCards(viewerId, users), total, pageNum, pageSize);
    }


    /**
     * 粉丝列表
     * 可在“设置”设置为他人可见/不可见
     * @param viewerId 查看粉丝列表者的id
     * @param userId 被查看粉丝列表者的id
     * @param pageNum 页码
     * @param pageSize 一页记录数
     * @return 返回粉丝列表用户卡片，条数，页码，一页记录数
     */
    @Override
    public PageBean<UserCardVO> listFans(Integer viewerId, Integer userId, int pageNum, int pageSize) {
        // 查询目标用户的粉丝总数
        long total = userFollowMapper.countFans(userId.longValue());
        // 查询目标用户的粉丝列表
        List<User> users = userFollowMapper.listFans(userId.longValue(), (pageNum - 1) * pageSize, pageSize);
        return new PageBean<>(buildCards(viewerId, users), total, pageNum, pageSize);
    }


    /**
     * 给关注/粉丝列表组装卡片，批量判断 isFollowing / isMutual，避免逐条查库
     */
    private List<UserCardVO> buildCards(Integer viewerId, List<User> users) {
        // 传入空列表直接返回空，不做任何 DB 查询
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> followingSet = new HashSet<>();
        Set<Long> mutualSet = new HashSet<>();
        if (viewerId != null) {
            // 查询当前用户关注的全部用户id合集
            followingSet.addAll(userFollowMapper.findFollowingIds(viewerId.longValue()));
            // 把 users 列表里每个用户的 id 抽出来，放进一个新的 List 里
            List<Long> ids = users.stream().map(u -> u.getId().longValue()).collect(Collectors.toList());
            // 在给定id集合中，哪些用户关注了当前用户（场景：互关判断）
            mutualSet.addAll(userFollowMapper.findMutualTargets(viewerId.longValue(), ids));
        }
        List<UserCardVO> cards = new ArrayList<>();
        for (User u : users) {
            long uid = u.getId().longValue();
            // followingSet里面是当前用户关注的全部用户的id合集
            // uid是传入的user列表的id
            // uid和followingSet里的id对得上(确实是我关注的） ---> true
            // ⚠ 自己查询自己的关注列表，我的关注和列表里的用户是否已关注肯定全部命中，如果是我看别人的列表，我的关注数据和他的关注列表可能就不一样（不会全部命中，甚至没有一个命中）
            boolean isFollowing = followingSet.contains(uid);
            // mutualSet是关注我的用户id，和传入的user列表对得上就是相互关注的
            // 我已关注  &&  所有关注我的id ？= 传入的用户列表id
            boolean isMutual = isFollowing && mutualSet.contains(uid);

            UserCardVO card = new UserCardVO();
            // 组装每一个卡片 标注清楚是否关注，是否互关
            card.setUserInfo(UserInfo.from(u));
            card.setIsFollowing(isFollowing);
            card.setIsMutual(isMutual);
            cards.add(card);
        }
        return cards;
    }
}
