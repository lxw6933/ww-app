package com.ww.app.redis.component;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.RedisTestApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;

/**
 * 点赞Redis组件测试类
 * 全面测试LikeRedisComponent的各项功能
 */
@Slf4j
@SpringBootTest(classes = RedisTestApplication.class)
public class LikeRedisComponentTest {

    @Resource
    private LikeRedisComponent likeRedisComponent;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 测试数据
    private final String contentType = "article";
    private final String contentId = "test_article_" + RandomUtil.randomNumbers(6);
    private final String userId1 = "user_" + RandomUtil.randomNumbers(6);
    private final String userId2 = "user_" + RandomUtil.randomNumbers(6);
    private final String userId3 = "user_" + RandomUtil.randomNumbers(6);

    @BeforeEach
    public void setUp() {
        log.info("开始测试，测试数据: contentType={}, contentId={}, userId1={}, userId2={}, userId3={}", 
                contentType, contentId, userId1, userId2, userId3);
        // 确保测试前数据干净
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        // 清理测试数据
        cleanup();
    }

    private void cleanup() {
        // 删除测试用的所有Redis键
        Set<String> keys = stringRedisTemplate.keys("like:*" + contentId + "*");
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        Set<String> userKeys = new HashSet<>();
        userKeys.add("like:user:" + userId1);
        userKeys.add("like:user:" + userId2);
        userKeys.add("like:user:" + userId3);
        stringRedisTemplate.delete(userKeys);
    }

    /**
     * 测试添加点赞功能
     */
    @Test
    public void testAddLike() {
        log.info("测试添加点赞功能");
        
        // 添加点赞
        boolean result = likeRedisComponent.addLike(userId1, contentType, contentId);
        Assert.isTrue(result, "添加点赞失败");
        
        // 验证点赞状态
        boolean hasLiked = likeRedisComponent.hasLiked(userId1, contentType, contentId);
        Assert.isTrue(hasLiked, "点赞状态校验失败");
        
        // 验证点赞数
        long likeCount = likeRedisComponent.getLikeCount(contentType, contentId);
        Assert.isTrue(likeCount == 1, "点赞数量校验失败，期望1，实际" + likeCount);
        
        log.info("添加点赞功能测试通过");
    }

    /**
     * 测试取消点赞功能
     */
    @Test
    public void testRemoveLike() {
        log.info("测试取消点赞功能");
        
        // 先添加点赞
        likeRedisComponent.addLike(userId1, contentType, contentId);
        
        // 取消点赞
        boolean result = likeRedisComponent.removeLike(userId1, contentType, contentId);
        Assert.isTrue(result, "取消点赞失败");
        
        // 验证点赞状态
        boolean hasLiked = likeRedisComponent.hasLiked(userId1, contentType, contentId);
        Assert.isFalse(hasLiked, "取消点赞后状态校验失败");
        
        // 验证点赞数
        long likeCount = likeRedisComponent.getLikeCount(contentType, contentId);
        Assert.isTrue(likeCount == 0, "点赞数量校验失败，期望0，实际" + likeCount);
        
        log.info("取消点赞功能测试通过");
    }

    /**
     * 测试重复点赞处理
     */
    @Test
    public void testDuplicateLike() {
        log.info("测试重复点赞处理");
        
        // 第一次点赞
        boolean firstLike = likeRedisComponent.addLike(userId1, contentType, contentId);
        Assert.isTrue(firstLike, "第一次点赞失败");
        
        // 重复点赞
        boolean duplicateLike = likeRedisComponent.addLike(userId1, contentType, contentId);
        Assert.isFalse(duplicateLike, "重复点赞应当返回false");
        
        // 验证点赞数（应该仍然是1）
        long likeCount = likeRedisComponent.getLikeCount(contentType, contentId);
        Assert.isTrue(likeCount == 1, "重复点赞后点赞数量校验失败，期望1，实际" + likeCount);
        
        log.info("重复点赞处理测试通过");
    }

    /**
     * 测试批量点赞数据处理
     */
    @Test
    public void testBatchLikeOperations() {
        log.info("测试批量点赞数据处理");
        
        // 添加多个用户点赞
        likeRedisComponent.addLike(userId1, contentType, contentId);
        likeRedisComponent.addLike(userId2, contentType, contentId);
        likeRedisComponent.addLike(userId3, contentType, contentId);
        
        // 验证点赞总数
        long likeCount = likeRedisComponent.getLikeCount(contentType, contentId);
        Assert.isTrue(likeCount == 3, "批量点赞后总数校验失败，期望3，实际" + likeCount);
        
        // 获取所有点赞用户
        Set<String> likedUsers = likeRedisComponent.getContentLikedUsers(contentType, contentId);
        Assert.isTrue(likedUsers.size() == 3, "点赞用户列表数量校验失败，期望3，实际" + likedUsers.size());
        Assert.isTrue(likedUsers.contains(userId1), "点赞用户列表应包含用户1");
        Assert.isTrue(likedUsers.contains(userId2), "点赞用户列表应包含用户2");
        Assert.isTrue(likedUsers.contains(userId3), "点赞用户列表应包含用户3");
        
        log.info("批量点赞数据处理测试通过");
    }

    /**
     * 测试获取用户点赞的内容
     */
    @Test
    public void testGetUserLikedContent() {
        log.info("测试获取用户点赞的内容");
        
        String contentId2 = contentId + "_2";
        
        // 用户点赞多个内容
        likeRedisComponent.addLike(userId1, contentType, contentId);
        likeRedisComponent.addLike(userId1, contentType, contentId2);
        
        // 获取用户点赞的内容列表
        Set<String> likedContent = likeRedisComponent.getUserLikedContent(userId1);
        Assert.isTrue(likedContent.size() == 2, "用户点赞内容列表数量校验失败，期望2，实际" + likedContent.size());
        Assert.isTrue(likedContent.contains(contentId), "用户点赞内容列表应包含内容1");
        Assert.isTrue(likedContent.contains(contentId2), "用户点赞内容列表应包含内容2");
        
        log.info("获取用户点赞的内容测试通过");
    }

    /**
     * 测试热门内容排行榜
     */
    @Test
    public void testHotContentRanking() {
        log.info("测试热门内容排行榜");
        
        String contentId2 = contentId + "_2";
        String contentId3 = contentId + "_3";
        
        // 为不同内容添加不同数量的点赞
        likeRedisComponent.addLike(userId1, contentType, contentId); // 内容1: 1个赞
        
        likeRedisComponent.addLike(userId1, contentType, contentId2); // 内容2: 2个赞
        likeRedisComponent.addLike(userId2, contentType, contentId2);
        
        likeRedisComponent.addLike(userId1, contentType, contentId3); // 内容3: 3个赞
        likeRedisComponent.addLike(userId2, contentType, contentId3);
        likeRedisComponent.addLike(userId3, contentType, contentId3);
        
        // 获取热门排行榜
        List<Map<String, Object>> ranking = likeRedisComponent.getHotContentRanking(contentType, 10);
        
        // 验证排行榜
        Assert.isTrue(ranking.size() == 3, "排行榜内容数量校验失败，期望3，实际" + ranking.size());
        
        // 验证排序（应该是按点赞数从高到低）
        Assert.isTrue(contentId3.equals(ranking.get(0).get("contentId")), "排行第一位应该是内容3");
        Assert.isTrue(contentId2.equals(ranking.get(1).get("contentId")), "排行第二位应该是内容2");
        Assert.isTrue(contentId.equals(ranking.get(2).get("contentId")), "排行第三位应该是内容1");
        
        // 验证点赞数
        Assert.isTrue(3L == (Long) ranking.get(0).get("likeCount"), "内容3的点赞数应为3");
        Assert.isTrue(2L == (Long) ranking.get(1).get("likeCount"), "内容2的点赞数应为2");
        Assert.isTrue(1L == (Long) ranking.get(2).get("likeCount"), "内容1的点赞数应为1");
        
        log.info("热门内容排行榜测试通过");
    }

    /**
     * 测试清理过期点赞数据
     */
    @Test
    public void testCleanExpiredLikeData() {
        log.info("测试清理过期点赞数据");
        
        // 添加点赞数据
        likeRedisComponent.addLike(userId1, contentType, contentId);
        
        // 清理数据（保留30天）
        long cleanedCount = likeRedisComponent.cleanExpiredLikeData(contentType, 30);
        log.info("清理了{}个过期键", cleanedCount);
        
        // 验证数据仍然存在
        boolean hasLiked = likeRedisComponent.hasLiked(userId1, contentType, contentId);
        Assert.isTrue(hasLiked, "清理后数据应该仍然存在");
        
        log.info("清理过期点赞数据测试通过");
    }

    /**
     * 测试参数验证
     */
    @Test
    public void testParameterValidation() {
        log.info("测试参数验证");
        
        boolean exceptionThrown = false;
        try {
            likeRedisComponent.addLike("", contentType, contentId);
        } catch (ApiException e) {
            exceptionThrown = true;
        }
        Assert.isTrue(exceptionThrown, "空参数应抛出异常");
        
        exceptionThrown = false;
        try {
            likeRedisComponent.getLikeCount("", "");
        } catch (ApiException e) {
            exceptionThrown = true;
        }
        Assert.isTrue(exceptionThrown, "空参数应抛出异常");
        
        log.info("参数验证测试通过");
    }
} 