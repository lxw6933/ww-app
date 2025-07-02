package com.ww.app.redis.pvuv;

import com.ww.app.redis.component.pvuv.RedisPvUvManager;
import com.ww.app.redis.component.pvuv.keys.PvUvRedisKeyBuilder;
import com.ww.app.redis.component.pvuv.report.RedisPvUvReport;
import com.ww.app.redis.component.pvuv.storage.RedisPvUvStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Redis PV/UV管理器测试类
 */
@SpringBootTest
public class RedisPvUvManagerTest {

    @Autowired(required = false)
    private RedisPvUvManager pvUvManager;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;
    
    @MockBean
    private PvUvRedisKeyBuilder keyBuilder;

    private RedisPvUvManager redisPvUvManager;

    @BeforeEach
    public void setUp() {
        // 设置mock行为，确保测试正确工作
        when(keyBuilder.buildPvKey(any(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            LocalDate date = invocation.getArgument(1);
            String dateStr = date != null ? date.toString() : LocalDate.now().toString();
            return "pv:" + key + ":" + dateStr;
        });
        
        when(keyBuilder.buildUvKey(any(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            LocalDate date = invocation.getArgument(1);
            String dateStr = date != null ? date.toString() : LocalDate.now().toString();
            return "uv:" + key + ":" + dateStr;
        });
        
        when(keyBuilder.buildEventKey(any())).thenAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            return "event:" + eventId;
        });
        
        RedisPvUvStorage redisStorage = new RedisPvUvStorage(stringRedisTemplate);
        redisPvUvManager = new RedisPvUvManager(redisStorage, keyBuilder);
    }

    @AfterEach
    public void tearDown() {
        if (redisPvUvManager != null) {
            redisPvUvManager.shutdown();
        }
    }

    @Test
    public void testRecordPvAndUv() {
        // 使用配置创建的管理器或手动创建的管理器
        RedisPvUvManager manager = pvUvManager != null ? pvUvManager : redisPvUvManager;
        
        // 测试记录PV
        String key = "test:page:index";
        long pv1 = manager.recordPv(key);
        assertEquals(1, pv1);
        
        long pv2 = manager.recordPv(key);
        assertEquals(2, pv2);
        
        // 测试记录UV
        String userId1 = "user1";
        long uv1 = manager.recordUv(key, userId1);
        assertEquals(1, uv1);
        
        // 同一用户多次访问，UV不变
        long uv2 = manager.recordUv(key, userId1);
        assertEquals(1, uv2);
        
        // 不同用户访问，UV增加
        String userId2 = "user2";
        long uv3 = manager.recordUv(key, userId2);
        assertEquals(2, uv3);
        
        // 测试同时记录PV和UV
        String userId3 = "user3";
        RedisPvUvManager.PvUvResult result = manager.recordPvAndUv(key, userId3);
        assertEquals(3, result.getPv());
        assertEquals(3, result.getUv());
        
        // 测试获取PV和UV
        assertEquals(3, manager.getPv(key));
        assertEquals(3, manager.getUv(key));
        
        RedisPvUvManager.PvUvResult getResult = manager.getPvAndUv(key);
        assertEquals(3, getResult.getPv());
        assertEquals(3, getResult.getUv());
    }

    @Test
    public void testBatchSync() {
        // 使用手动创建的管理器确保测试环境一致
        RedisPvUvManager manager = redisPvUvManager;
        
        // 记录多个PV和UV
        String key = "test:batch:sync";
        for (int i = 0; i < 10; i++) {
            manager.recordPv(key);
            manager.recordUv(key, "user" + i);
        }
        
        // 验证本地计数
        assertEquals(10, manager.getPv(key));
        assertEquals(10, manager.getUv(key));
        
        // 手动触发同步
        manager.syncToRedisNow();
    }

    @Test
    public void testReport() {
        // 使用配置创建的管理器或手动创建的管理器
        RedisPvUvManager manager = pvUvManager != null ? pvUvManager : redisPvUvManager;
        RedisPvUvReport report = new RedisPvUvReport(manager);
        
        // 生成测试数据
        String key = "test:report";
        LocalDate today = LocalDate.now();
        
        // 当天数据
        for (int i = 0; i < 5; i++) {
            manager.recordPv(key);
            manager.recordUv(key, "today_user_" + i);
        }
        
        // 昨天数据
        LocalDate yesterday = today.minusDays(1);
        for (int i = 0; i < 3; i++) {
            manager.recordPv(key, yesterday);
            manager.recordUv(key, "yesterday_user_" + i, yesterday);
        }
        
        // 生成日报表
        Map<LocalDate, Long> dailyPvReport = report.getDailyPvReport(key, 2);
        assertEquals(2, dailyPvReport.size());
        assertEquals(5, dailyPvReport.get(today));
        assertEquals(3, dailyPvReport.get(yesterday));
        
        Map<LocalDate, Long> dailyUvReport = report.getDailyUvReport(key, 2);
        assertEquals(2, dailyUvReport.size());
        assertEquals(5, dailyUvReport.get(today));
        assertEquals(3, dailyUvReport.get(yesterday));
    }
} 