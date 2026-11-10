package com.guat.cloud.user.service.impl;

import com.guat.cloud.user.dto.CommentCardVO;
import com.guat.cloud.user.dto.PageBean;
import com.guat.cloud.user.dto.PostCardVO;
import com.guat.cloud.user.entity.News;
import com.guat.cloud.user.entity.UserCollect;
import com.guat.cloud.user.exception.BusinessException;
import com.guat.cloud.user.mapper.CommentMapper;
import com.guat.cloud.user.mapper.NewsMapper;
import com.guat.cloud.user.mapper.UserCollectMapper;
import com.guat.cloud.user.service.CollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectServiceImpl implements CollectService {

    @Autowired
    private UserCollectMapper userCollectMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 收藏/取消收藏切换方法
     * @param userId 当前操作用户ID
     * @param collectType 收藏类型：1帖子，2评论
     * @param targetId 被收藏目标ID（帖子ID/评论ID）
     * @return 返回map，包含是否收藏成功标识、收藏总数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCollect(Integer userId, Integer collectType, Long targetId) {
        // 判断收藏类型是否为空，并且校验只能是1或者2
        if (collectType == null || (collectType != 1 && collectType != 2)) {
            // 抛出业务异常，提示收藏类型错误
            throw new BusinessException("收藏类型错误");
        }
        // 如果收藏类型为1，代表收藏帖子
        if (collectType == 1) {
            // 根据帖子id查询帖子，判断帖子是否存在
            if (newsMapper.findById(targetId.intValue()) == null) {
                // 帖子不存在，抛出业务异常
                throw new BusinessException("帖子不存在");
            }
        } else {
            // 否则收藏类型为2，代表收藏评论
            // 根据评论id查询评论，判断评论是否存在
            if (commentMapper.selectById(targetId) == null) {
                // 评论不存在，抛出业务异常
                throw new BusinessException("评论不存在");
            }
        }
        // 定义delta变量，用于更新收藏数量，+1收藏，-1取消收藏
        int delta;
        // 查询当前用户，该类型，该目标的收藏记录
        UserCollect record = userCollectMapper.findByUserAndTarget(userId.longValue(), collectType, targetId);
        // 如果查询不到收藏记录，说明从未收藏过
        if (record == null) {
            // 创建收藏实体对象
            UserCollect uc = new UserCollect();
            // 设置收藏用户id，转Long类型
            uc.setUserId(userId.longValue());
            // 设置收藏类型
            uc.setCollectType(collectType);
            // 设置被收藏目标id
            uc.setTargetId(targetId);
            // 设置isCancel=0，代表有效收藏
            uc.setIsCancel(0);
            // 插入一条新收藏记录到数据库
            userCollectMapper.insert(uc);
            // 收藏成功，数量+1
            delta = 1;
        } else if (Integer.valueOf(1).equals(record.getIsCancel())) {
            // 查询到收藏记录，但isCancel=1，代表之前取消收藏，现在重新收藏
            // 更新这条记录，isCancel改为0，恢复收藏
            userCollectMapper.updateCancel(record.getId(), 0);
            // 重新收藏，数量+1
            delta = 1;
        } else {
            // 查询到有效收藏记录，本次操作是取消收藏
            // 更新这条记录，isCancel改为1，标记为取消收藏
            userCollectMapper.updateCancel(record.getId(), 1);
            // 取消收藏，数量-1
            delta = -1;
        }
        // 创建Map，封装返回结果
        Map<String, Object> result = new HashMap<>();
        // delta>0代表本次操作是收藏，存入是否已收藏标识
        result.put("isCollected", delta > 0);
        // 帖子收藏同步 news.collect_count 冗余计数
        // 如果本次操作对象是帖子，需要更新帖子表收藏总数
        if (collectType == 1) {
            // 执行sql更新帖子收藏数量，传入delta增减值
            newsMapper.updateCollectCount(targetId.intValue(), delta);
            // 重新查询帖子信息，拿到最新收藏数
            News n = newsMapper.findById(targetId.intValue());
            // 判断帖子和收藏数是否为空，兜底取值，存入结果map
            result.put("collectCount", n == null || n.getCollectCount() == null ? Math.max(delta, 0) : n.getCollectCount());
        }
        // 返回结果map
        return result;
    }

    /**
     * 获取收藏状态接口
     * @param viewerId 当前查看用户id，未登录为null
     * @param collectType 收藏类型：1帖子，2评论
     * @param targetId 目标帖子/评论id
     * @return map，包含isCollected是否收藏标识
     */
    @Override
    public Map<String, Object> getCollectStatus(Integer viewerId, Integer collectType, Long targetId) {
        // 判断收藏类型是否为空，并且校验只能是1或者2
        if (collectType == null || (collectType != 1 && collectType != 2)) {
            // 抛出业务异常，提示收藏类型错误
            throw new BusinessException("收藏类型错误");
        }
        // 初始化收藏状态，默认未收藏false
        boolean isCollected = false;
        // 判断用户id不为空，代表用户已登录
        if (viewerId != null) {
            // 查询该用户对应类型、对应目标的收藏记录
            UserCollect record = userCollectMapper.findByUserAndTarget(viewerId.longValue(), collectType, targetId);
            // 记录存在并且isCancel=0有效收藏，则状态置true
            isCollected = record != null && Integer.valueOf(0).equals(record.getIsCancel());
        }
        // 创建Map封装返回数据
        Map<String, Object> result = new HashMap<>();
        // 将收藏状态放入map
        result.put("isCollected", isCollected);
        // 返回结果map
        return result;
    }

    /**
     * 查询我的收藏帖子列表，分页
     * @param userId 当前登录用户id
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return PageBean分页对象，里面存放PostCardVO帖子卡片集合、总条数
     */
    @Override
    public PageBean<PostCardVO> listMyCollectedPosts(Integer userId, int pageNum, int pageSize) {
        // 查询该用户收藏帖子的总记录数
        long total = userCollectMapper.countCollectedPosts(userId.longValue());
        // 查询分页的收藏帖子列表，计算偏移量(pageNum-1)*pageSize
        List<PostCardVO> list = userCollectMapper.listCollectedPosts(userId.longValue(), (pageNum - 1) * pageSize, pageSize);
        // 组装分页对象返回
        return new PageBean<>(list, total, pageNum, pageSize);
    }

    /**
     * 查询我的收藏评论列表，分页
     * @param userId 当前登录用户id
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return PageBean分页对象，里面存放CommentCardVO评论卡片集合、总条数
     */
    @Override
    public PageBean<CommentCardVO> listMyCollectedComments(Integer userId, int pageNum, int pageSize) {
        // 查询该用户收藏评论的总记录数
        long total = userCollectMapper.countCollectedComments(userId.longValue());
        // 查询分页的收藏评论列表，计算偏移量(pageNum-1)*pageSize
        List<CommentCardVO> list = userCollectMapper.listCollectedComments(userId.longValue(), (pageNum - 1) * pageSize, pageSize);
        // 组装分页对象返回
        return new PageBean<>(list, total, pageNum, pageSize);
    }

}
