package com.ww.mall.promotion.entity.group;

import com.ww.mall.promotion.enums.GroupActivityStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 拼团活动实体测试。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 校验活动状态改为实时推导后的边界行为
 */
class GroupActivityTest {

    /**
     * 应根据开始时间和结束时间实时推导活动状态。
     */
    @Test
    void shouldResolveStatusByStartAndEndTime() {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        GroupActivity notStarted = new GroupActivity();
        notStarted.setStartTime(new Date(nowMillis + 60_000L));
        notStarted.setEndTime(new Date(nowMillis + 120_000L));
        assertEquals(GroupActivityStatus.NOT_STARTED.getCode(), notStarted.resolveStatus(now));

        GroupActivity active = new GroupActivity();
        active.setStartTime(new Date(nowMillis - 60_000L));
        active.setEndTime(new Date(nowMillis + 60_000L));
        assertEquals(GroupActivityStatus.ACTIVE.getCode(), active.resolveStatus(now));

        GroupActivity ended = new GroupActivity();
        ended.setStartTime(new Date(nowMillis - 120_000L));
        ended.setEndTime(new Date(nowMillis - 60_000L));
        assertEquals(GroupActivityStatus.ENDED.getCode(), ended.resolveStatus(now));
    }
}
