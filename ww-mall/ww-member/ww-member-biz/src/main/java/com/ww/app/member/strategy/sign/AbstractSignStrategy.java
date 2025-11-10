package com.ww.app.member.strategy.sign;

import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.component.SignComponent;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import com.ww.app.member.entity.mongo.MemberSignRecord;
import com.ww.app.member.strategy.sign.time.SignBitmapStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-07-21- 09:16
 * @description: 签到策略抽象类
 */
@Slf4j
public abstract class AbstractSignStrategy implements SignBitmapStrategy, SignStrategy, ResignStrategy {

    @Resource
    protected StringRedisTemplate stringRedisTemplate;

    @Resource
    protected SignRedisKeyBuilder signRedisKeyBuilder;

    @Resource
    protected SignComponent signComponent;

    @Override
    public int doSign(LocalDate date, ClientUser clientUser) {
        // 是否为补签标识
        boolean resignFlag = date.equals(LocalDate.now());

        // 获取偏移量
        int offset = getOffset(date);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // 查看是否已签到
        boolean isSigned = isSigned(signKey, offset);
        if (isSigned) {
            return getContinuousSignCount(date, clientUser);
        }

        // TODO LUA script替换
        if (resignFlag) {
            // 构建补签计数器Key
            String countKey = buildResignCountKey(clientUser.getId(), date);
            // 获取已使用的补签次数
            int usedCount = getResignCount(countKey);
            int remainingCount = Math.max(0, getResignConfig() - usedCount);
            if (remainingCount == 0) {
                throw new ApiException("本月补签次数已用完");
            }
            // 更新补签次数
            Long currentCount = stringRedisTemplate.opsForValue().increment(countKey, 1);

            // 首次设置过期时间(到当前周期最后一天)
            if (currentCount != null && currentCount == 1) {
                long seconds = getResignKeyExpireTime();
                stringRedisTemplate.expire(countKey, seconds, TimeUnit.SECONDS);
            }
            // 记录补签日志
            log.info("用户{}成功补签日期{}", clientUser.getId(), date);
        }

        // 签到
        sign(signKey, offset);

        // 处理签到奖励
        processSignReward(clientUser.getId(), date);

        // 统计连续签到的次数
        return getContinuousSignCount(date, clientUser);
    }

    @Override
    public int getContinuousSignCount(LocalDate date, ClientUser clientUser) {
        // 获取位数[当前周期内有多少位]
        int bits = getBitCount(date);

        // 获取当前位置[获取当前时间在哪个bit位上]
        int position = getOffset(date);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        return getBitmapStreakSignCount(signKey, bits, position);
    }

    @Override
    public int getSignCount(LocalDate date, ClientUser clientUser) {
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        return getBitmapSignCount(signKey);
    }

    @Override
    public Map<LocalDate, Boolean> getSignInfo(LocalDate date, ClientUser clientUser) {
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // 构建一个自动排序的 Map
        Map<LocalDate, Boolean> signInfo = new TreeMap<>();

        // 获取位数
        int bits = getBitCount(date);

        // 获取签到数据
        List<Long> list = getBitmapSignInfo(signKey, bits);
        if (list == null || list.isEmpty()) {
            return signInfo;
        }

        long v = list.get(0) == null ? 0 : list.get(0);

        // 填充签到信息
        fillSignInfo(date, v, signInfo);

        return signInfo;
    }

    @Override
    public List<Boolean> getSignDetailInfo(ClientUser clientUser) {
        // 获取日期
        LocalDate date = LocalDate.now();
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);
        // 获取当前签到bitmap
        byte[] signBytes = getSignBytes(signKey);
        if (signBytes == null) {
            return Collections.emptyList();
        }
        // 获取位数
        int bits = getBitCount(date);
        return signComponent.decodeHexBitmapToBooleans(MemberSignRecord.encodeBitmap(signBytes), bits);
    }

    public int getResignCount(Long userId, LocalDate date) {
        String resignCountKey = buildResignCountKey(userId, date);
        return getResignCount(resignCountKey);
    }

    public int getResignCount(String resignCountKey) {
        String countStr = stringRedisTemplate.opsForValue().get(resignCountKey);
        return countStr == null ? 0 : Integer.parseInt(countStr);
    }

    /**
     * 处理签到奖励
     */
    protected abstract void processSignReward(Long userId, LocalDate date);

    /**
     * 构建签到Key
     */
    protected abstract String buildSignKey(Long userId, LocalDate date);

    /**
     * 填充签到信息
     */
    @Deprecated
    protected void fillSignInfo(LocalDate date, long bitValue, Map<LocalDate, Boolean> signInfo) {}

    /**
     * 是否签到
     */
    public boolean isSigned(String signKey, int offset) {
        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(signKey, offset);
        return Boolean.TRUE.equals(isSigned);
    }

    /**
     * 签到
     */
    public boolean sign(String signKey, int offset) {
        Boolean sign = stringRedisTemplate.opsForValue().setBit(signKey, offset, true);
        return Boolean.TRUE.equals(sign);
    }

    /**
     * 获取签到总次数
     */
    public int getBitmapSignCount(String signKey) {
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
    public List<Long> getBitmapSignInfo(String signKey, int bits) {
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
    public int getBitmapStreakSignCount(String signKey, int bits, int position) {
        List<Long> list = getBitmapSignInfo(signKey, bits);
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
     * 将bitmapBytes重放到bitmap
     */
    public boolean restoreSignBitmap(String signKey, byte[] bitmapBytes) {
        // 使用 Redis 的 SET 命令直接设置整个 bitmap
        return Boolean.TRUE.equals(stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.set(signKey.getBytes(StandardCharsets.UTF_8), bitmapBytes);
            return true;
        }));
    }

}