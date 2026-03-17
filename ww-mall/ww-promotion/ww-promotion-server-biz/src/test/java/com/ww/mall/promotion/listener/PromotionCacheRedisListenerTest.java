package com.ww.mall.promotion.listener;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.mall.promotion.entity.group.GroupActivity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.test.util.ReflectionTestUtils;

import static com.ww.mall.promotion.constants.RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL;
import static org.mockito.Mockito.verify;

/**
 * 拼团活动缓存失效监听测试。
 *
 * @author ww
 * @create 2026-03-16
 * @description: 校验缓存失效监听使用字符串活动ID失效本地缓存
 */
@ExtendWith(MockitoExtension.class)
class PromotionCacheRedisListenerTest {

    @Mock
    private LoadingCache<String, GroupActivity> groupActivityCache;

    /**
     * 应使用字符串活动ID进行缓存失效。
     */
    @Test
    void shouldInvalidateGroupActivityCacheByStringKey() {
        PromotionCacheRedisListener listener = new PromotionCacheRedisListener();
        ReflectionTestUtils.setField(listener, "groupActivityCache", groupActivityCache);

        listener.onMessage(new DefaultMessage(
                GROUP_ACTIVITY_CACHE_CHANNEL.getBytes(),
                "activity-1001".getBytes()
        ), null);

        verify(groupActivityCache).invalidate("activity-1001");
    }
}
