package com.ww.app.member.strategy.sign;

import cn.hutool.core.date.DatePattern;
import com.ww.app.common.common.ClientUser;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ww
 * @create 2023-07-21- 09:16
 * @description: 签到策略抽象类
 */
public abstract class AbstractSignStrategy implements SignStrategy {

    @Resource
    protected StringRedisTemplate stringRedisTemplate;

    @Resource
    protected SignRedisKeyBuilder signRedisKeyBuilder;

    @Override
    public int doSign(String dateStr, ClientUser clientUser) {
        // 获取当前日期
        LocalDate date = parseDate(dateStr);

        // 获取偏移量
        int offset = getOffset(date);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // 查看是否已签到
        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(signKey, offset);
        if (Boolean.TRUE.equals(isSigned)) {
            return getContinuousSignCount(dateStr, clientUser);
        }

        // 签到
        stringRedisTemplate.opsForValue().setBit(signKey, offset, true);

        // 处理签到奖励
        processSignReward(clientUser.getId(), date);

        // 统计连续签到的次数
        return getContinuousSignCount(dateStr, clientUser);
    }

    @Override
    public int getContinuousSignCount(String dateStr, ClientUser clientUser) {
        // 获取日期
        LocalDate date = parseDate(dateStr);

        // 获取位数
        int bits = getBitCount(date);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // bitfield 命令获取签到数据
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(bits))
                .valueAt(0);

        List<Long> list = stringRedisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int signCount = 0;
        long v = list.get(0) == null ? 0 : list.get(0);

        // 获取当前位置
        int position = getOffset(date);

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

    @Override
    public int getSignCount(String dateStr, ClientUser clientUser) {
        // 获取日期
        LocalDate date = parseDate(dateStr);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // bitcount 命令
        Long signCount = stringRedisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(signKey.getBytes())
        );

        return signCount == null ? 0 : signCount.intValue();
    }

    @Override
    public Map<String, Boolean> getSignInfo(String dateStr, ClientUser clientUser) {
        // 获取日期
        LocalDate date = parseDate(dateStr);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // 构建一个自动排序的 Map
        Map<String, Boolean> signInfo = new TreeMap<>();

        // 获取位数
        int bits = getBitCount(date);

        // bitfield 命令获取签到数据
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(bits))
                .valueAt(0);

        // 从偏移量offset=0开始取bits位，获取无符号整数的值
        List<Long> list = stringRedisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return signInfo;
        }

        long v = list.get(0) == null ? 0 : list.get(0);

        // 填充签到信息
        fillSignInfo(date, v, signInfo);

        return signInfo;
    }

    /**
     * 解析日期
     */
    protected LocalDate parseDate(String dateStr) {
        return dateStr == null ? LocalDate.now() : LocalDate.parse(dateStr, DatePattern.NORM_DATE_FORMATTER);
    }

    /**
     * 处理签到奖励
     */
    protected abstract void processSignReward(Long userId, LocalDate date);

    /**
     * 获取偏移量
     */
    protected abstract int getOffset(LocalDate date);

    /**
     * 获取位数
     */
    protected abstract int getBitCount(LocalDate date);

    /**
     * 构建签到Key
     */
    protected abstract String buildSignKey(Long userId, LocalDate date);

    /**
     * 填充签到信息
     */
    protected abstract void fillSignInfo(LocalDate date, long bitValue, Map<String, Boolean> signInfo);
} 