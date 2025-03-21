package com.ww.app.member.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.service.SignService;
import com.ww.app.redis.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ww
 * @create 2023-07-21- 09:17
 * @description: 签到业务实现类
 */
@Slf4j
@Service
public class SignServiceImpl implements SignService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    @DistributedLock(enableUserLock = true, operationKey = "'sign'", waitTime = 3, leaseTime = 3)
    public int doSign(String dateStr, ClientUser clientUser) {
        Date now = new Date();
        Date date = getDate(dateStr);
        // 获得指定日期是所在月份的第几天：2023-06-14，返回14，代表这个月份的第14天
        int dayOfMonth = DateUtil.dayOfMonth(date);
        if (dateStr != null) {
            if (DateUtil.month(date) != DateUtil.month(now) || DateUtil.year(date) != DateUtil.year(now)) {
                throw new ApiException("只能补签当前月的日期");
            }
            if (date.after(now)) {
                throw new ApiException("还未到签到时间");
            }
        }
        // 偏移量 offset 从 0 开始
        int offset = dayOfMonth - 1;
        // 构建 Key user:sign:5:yyyyMM
        String signKey = buildSignKey(clientUser.getId(), date);
        // 查看是否已签到
        Boolean isSigned = redisTemplate.opsForValue().getBit(signKey, offset);
        if (Boolean.TRUE.equals(isSigned)) {
            log.warn("当前日期已完成签到，无需再签");
            throw new ApiException("当前日期已完成签到，无需再签");
        }
        // 签到
        redisTemplate.opsForValue().setBit(signKey, offset, true);
        // 统计连续签到的次数
        return getContinuousSignCount(dateStr, clientUser);
    }

    @Override
    public int getContinuousSignCount(String dateStr, ClientUser clientUser) {
        Date date = getDate(dateStr);
        // 获取日期对应的天数，多少号，假设是 30
        int dayOfMonth = DateUtil.dayOfMonth(date);
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);
        // bitfield user:sign:5:202011 u30 0
        BitFieldSubCommands bitFieldSubCommands
                = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int signCount = 0;
        long v = list.get(0) == null ? 0 : list.get(0);
        for (int i = dayOfMonth; i > 0; i--) {
            // i 表示位移操作次数
            // 右移再左移，如果等于自己说明最低位是 0，表示未签到
            if (v >> 1 << 1 == v) {
                // 低位 0 且非当天说明连续签到中断了
                if (i != dayOfMonth) {
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
        Date date = getDate(dateStr);
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);
        // bitcount user:sign:5:202011
        Long signCount = redisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(signKey.getBytes())
        );
        return signCount == null ? 0 : signCount.intValue();
    }

    @Override
    public Map<String, Boolean> getSignInfo(String dateStr, ClientUser clientUser) {
        Date date = getDate(dateStr);
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);
        // 构建一个自动排序的 Map
        Map<String, Boolean> signInfo = new TreeMap<>();
        // 获取月份，从0开始：0-11
        int month = DateUtil.month(date);
        // 是否是闰年
        boolean leapYear = DateUtil.isLeapYear(DateUtil.year(date));
        // 获取某月的总天数（考虑闰年）
        int dayOfMonth = DateUtil.lengthOfMonth(month + 1, leapYear);
        // bitfield user:sign:5:202011 u30 0
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);
        // 从偏移量offset=0开始取dayOfMonth位，获取无符号整数的值
        List<Long> list = redisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return signInfo;
        }
        long v = list.get(0) == null ? 0 : list.get(0);
        // 从低位到高位进行遍历，为 0 表示未签到，为 1 表示已签到
        for (int i = dayOfMonth; i > 0; i--) {
            /*
                签到：  yyyy-MM-01 true
                未签到：yyyy-MM-01 false
             */
            LocalDateTime localDateTime = LocalDateTimeUtil.of(date).withDayOfMonth(i);
            // 先右移再左移，如果不等于自己说明签到，否则未签到
            boolean flag = v >> 1 << 1 != v;
            // 存放当月每天的签到情况
            signInfo.put(DateUtil.format(localDateTime, DatePattern.NORM_DATE_PATTERN), flag);
            v >>= 1;
        }
        return signInfo;
    }

    /**
     * 构建 Key -- user:sign:5:yyyyMM
     *
     * @param userId 用户id
     * @param date   签到日期
     * @return redis key
     */
    private String buildSignKey(Long userId, Date date) {
        return String.format("user:sign:%d:%s", userId, DateUtil.format(date, DatePattern.SIMPLE_MONTH_PATTERN));
    }

    /**
     * 获取日期
     *
     * @param dateStr 日期字符串 yyyy-MM-dd
     * @return 日期格式
     */
    private Date getDate(String dateStr) {
        if (StringUtils.isEmpty(dateStr)) {
            return new Date();
        }
        try {
            return DateUtil.parseDate(dateStr);
        } catch (Exception e) {
            log.error("日期格式解析失败：{}", dateStr);
            throw new ApiException(GlobalResCodeConstants.BAD_REQUEST);
        }
    }

}
