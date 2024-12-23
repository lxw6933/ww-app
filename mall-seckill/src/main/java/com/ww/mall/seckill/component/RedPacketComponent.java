package com.ww.mall.seckill.component;

import cn.hutool.core.collection.ListUtil;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.MoneyUtils;
import com.ww.mall.seckill.component.key.SeckillRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2024-12-23- 10:42
 * @description:
 */
@Slf4j
@Component
public class RedPacketComponent {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillRedisKeyBuilder seckillRedisKeyBuilder;

    private static final int RED_PACKET_BATCH_NUM = 100;

    public boolean generateRedPacket(String redPacketCode, BigDecimal totalAmount, int totalCount) {
        try {
            String redPacketKey = seckillRedisKeyBuilder.buildRedPacketKey(redPacketCode);
            BoundListOperations<String, String> redPacketOperation = stringRedisTemplate.opsForList().getOperations().boundListOps(redPacketKey);
            List<BigDecimal> redPacketAmounts = MoneyUtils.splitRedPacket(totalAmount, totalCount);
            ListUtil.page(redPacketAmounts, RED_PACKET_BATCH_NUM, targetList -> {
                String[] dataArr = targetList.stream().map(BigDecimal::toString)
                        .collect(Collectors.toList())
                        .toArray(new String[]{});
                redPacketOperation.leftPushAll(dataArr);
            });
            Long redPacketCount = redPacketOperation.size();
            return redPacketCount != null && redPacketCount == totalCount;
        } catch (Exception e) {
            log.error("生成红包异常", e);
            throw new ApiException("生成红包失败");
        }
    }

    public String receiveRedPacket(String redPacketCode) {
        String redPacketKey = seckillRedisKeyBuilder.buildRedPacketKey(redPacketCode);
        return stringRedisTemplate.opsForList().rightPop(redPacketKey);
    }

}
