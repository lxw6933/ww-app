package com.ww.app.member.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-10-16 17:03
 * @description:
 */
@Slf4j
@Component
public class SignComponent {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 记录补签次数
     */
    public void incrResignCount(String countKey) {
        // 获取当前计数
        int currentCount = getResignCount(countKey);
        // 如果是本月第一次补签，设置过期时间（到本月底）
        if (currentCount == 0) {
            // 计算到月底的过期时间
            LocalDate now = LocalDate.now();
            LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
            long seconds = now.until(endOfMonth.plusDays(1), java.time.temporal.ChronoUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(countKey, String.valueOf(1), seconds, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().increment(countKey, 1);
        }
    }

    /**
     * 获取补签次数
     */
    public int getResignCount(String countKey) {
        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        return countStr == null ? 0 : Integer.parseInt(countStr);
    }

    /**
     * 签到
     */
    public boolean sign(String signKey, int offset) {
        Boolean sign = stringRedisTemplate.opsForValue().setBit(signKey, offset, true);
        return Boolean.TRUE.equals(sign);
    }

    /**
     * 是否签到
     */
    public boolean isSigned(String signKey, int offset) {
        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(signKey, offset);
        return Boolean.TRUE.equals(isSigned);
    }

    /**
     * 获取签到总次数
     */
    public int getSignCount(String signKey) {
        Long signCount = stringRedisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(signKey.getBytes(StandardCharsets.UTF_8))
        );
        return signCount == null ? 0 : signCount.intValue();
    }

    /**
     * 获取签到信息 bitmap byte[]
     */
    public byte[] getSignBytes(String signKey) {
        return stringRedisTemplate.execute(
                (RedisCallback<byte[]>) conn -> conn.get(signKey.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 获取签到信息
     */
    public List<Long> getSignInfo(String signKey, int bits) {
        // bitfield 命令获取签到数据
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(bits))
                .valueAt(0);

        // 从偏移量offset=0开始取bits位，获取无符号整数的值
        return stringRedisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
    }

    /**
     * 获取连续签到次数
     */
    public int getStreakSignCount(String signKey, int bits, int position) {
        List<Long> list = getSignInfo(signKey, bits);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int signCount = 0;
        long v = list.get(0) == null ? 0 : list.get(0);

        for (int i = position + 1; i > 0; i--) {
            // 右移再左移，如果等于自己说明最低位是 0，表示未签到
            if (v >> 1 << 1 == v) {
                // 低位 0 且非当天说明连续签到中断了
                if (i != position + 1) {
                    break;
                }
            } else {
                signCount++;
            }
            // 右移一位并重新赋值，相当于把最低位丢弃一位
            v >>= 1;
        }
        return signCount;
    }

}
