package com.ww.app.redpacket.component;

import cn.hutool.core.collection.ListUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.MoneyUtils;
import com.ww.app.redpacket.common.RedpacketConstant;
import com.ww.app.redpacket.component.key.RedpacketRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ww.app.common.utils.CollectionUtils.convertList;

/**
 * @author ww
 * @create 2024-12-23- 10:42
 * @description:
 */
@Slf4j
@Component
public class RedpacketComponent {

    private static final int RED_PACKET_BATCH_NUM = 100;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedpacketRedisKeyBuilder redPacketRedisKeyBuilder;

    /**
     * 拆分红包到redis
     */
    public boolean generateRedpacket(String redPacketId, BigDecimal totalAmount, int totalCount) {
        try {
            String redPacketKey = redPacketRedisKeyBuilder.buildRedpacketKey(redPacketId);
            BoundListOperations<String, String> redPacketOperation = stringRedisTemplate.opsForList().getOperations().boundListOps(redPacketKey);
            List<BigDecimal> redPacketAmounts = MoneyUtils.splitRedPacket(totalAmount, totalCount);
            ListUtil.page(redPacketAmounts, RED_PACKET_BATCH_NUM, targetList -> {
                String[] redPacketAmountArr = convertList(targetList, BigDecimal::toString).toArray(new String[]{});
                redPacketOperation.leftPushAll(redPacketAmountArr);
            });
            redPacketOperation.expire(RedpacketConstant.REDPACKET_EXPIRE_TIME * 2, TimeUnit.SECONDS);
            Long redPacketCount = redPacketOperation.size();
            return redPacketCount != null && redPacketCount == totalCount;
        } catch (Exception e) {
            log.error("生成红包异常", e);
            throw new ApiException("生成红包失败");
        }
    }

    /**
     * 领取redis红包
     */
    public String receiveRedpacket(String redPacketId) {
        String redPacketKey = redPacketRedisKeyBuilder.buildRedpacketKey(redPacketId);
        return stringRedisTemplate.opsForList().rightPop(redPacketKey);
    }

    public List<String> getRemainRedpacket(String redPacketId) {
        String redPacketKey = redPacketRedisKeyBuilder.buildRedpacketKey(redPacketId);
        BoundListOperations<String, String> listOperations = stringRedisTemplate.opsForList().getOperations().boundListOps(redPacketKey);
        Long size = listOperations.size();
        if (size != null && size > 0) {
            return listOperations.rightPop(size);
        }
        return null;
    }

    public void removeRedpacket(String redPacketId) {
        String redPacketKey = redPacketRedisKeyBuilder.buildRedpacketKey(redPacketId);
        stringRedisTemplate.delete(redPacketKey);
    }

}
