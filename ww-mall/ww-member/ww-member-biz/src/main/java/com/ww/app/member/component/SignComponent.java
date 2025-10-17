package com.ww.app.member.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
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

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 记录补签次数
     */
    public void incrResignCount(String countKey) {
        // 获取当前计数
        int currentCount = getResignCount(countKey);
        // 如果是本月第一次补签，设置过期时间（到本月底）
        if (currentCount == 0) {
            long seconds = getSecondsToEndOfMonth();
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

        if (position < 0 || bits <= 0 || position >= bits) {
            return 0;
        }

        long v = list.get(0) == null ? 0 : list.get(0);

        // Redis 的 bit 编号以字节的高位为 offset 较小的位。
        // 通过 BITFIELD u{bits} #0 取回的整数，最先读取的位（offset=0）对应返回整数的最高位。
        // 因此将“当天所在位置”的位对齐到最低位，需要右移 (bits - 1 - position)。
        v >>= (bits - 1 - position);

        int signCount = 0;
        while ((v & 1L) == 1L) { // 当天未签到则直接返回 0
            signCount++;
            v >>= 1;
        }
        return signCount;
    }

    /**
     * 获取当前时间到本月底的剩余秒数
     *
     * @return 到本月底的剩余秒数
     */
    public static long getSecondsToEndOfMonth() {
        // 使用 LocalDateTime 获取当前时间
        LocalDateTime now = LocalDateTime.now();

        // 获取本月的最后一天的最后时刻
        LocalDateTime endOfMonth = now.toLocalDate()
                .with(TemporalAdjusters.lastDayOfMonth())
                .atTime(LocalTime.MAX);

        // 计算时间差（秒）
        return Duration.between(now, endOfMonth).getSeconds();
    }

    /**
     * 获取当前时间到本周底的剩余秒数
     *
     * @return 到本周底的剩余秒数
     */
    public static long getSecondsToEndOfWeek() {
        LocalDateTime now = LocalDateTime.now();

        // 获取本周的最后一天（周日）的最后时刻
        LocalDateTime endOfWeek = now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .atTime(LocalTime.MAX);

        return Duration.between(now, endOfWeek).getSeconds();
    }

}
