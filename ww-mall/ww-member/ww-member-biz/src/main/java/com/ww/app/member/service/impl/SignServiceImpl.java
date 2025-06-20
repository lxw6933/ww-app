package com.ww.app.member.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import com.ww.app.member.service.SignService;
import com.ww.app.member.strategy.sign.SignStrategy;
import com.ww.app.member.strategy.sign.SignStrategyFactory;
import com.ww.app.member.util.SignDateValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-07-21- 09:17
 * @description: 签到业务实现类
 */
@Slf4j
@Service
public class SignServiceImpl implements SignService {

    /**
     * 用户每月补签次数上限
     */
    private static final int MONTHLY_RESIGN_LIMIT = 3;

    @Resource
    private SignStrategyFactory signStrategyFactory;

    @Resource
    private SignDateValidator signDateValidator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SignRedisKeyBuilder signRedisKeyBuilder;

    @Override
    public int doSign(String date, ClientUser clientUser) {
        // 判断是签到还是补签
        if (StrUtil.isBlank(date)) {
            // 日期为空，执行当天签到
            return doTodaySign(clientUser);
        } else {
            // 日期不为空，执行指定日期签到（可能是补签）
            return doSpecificDateSign(date, clientUser);
        }
    }

    /**
     * 执行当天签到
     */
    private int doTodaySign(ClientUser clientUser) {
        // 获取当天日期字符串
        String today = LocalDate.now().format(DatePattern.NORM_DATE_FORMATTER);
        // 检查是否已签到
        if (isSignedOn(today, clientUser)) {
            throw new ApiException("今日已签到，请勿重复签到");
        }
        // 使用策略模式进行签到
        SignStrategy strategy = signStrategyFactory.getDefaultStrategy();
        return strategy.doSign(today, clientUser);
    }

    /**
     * 执行指定日期签到（包括补签）
     */
    private int doSpecificDateSign(String date, ClientUser clientUser) {
        LocalDate today = LocalDate.now();
        LocalDate signDate = LocalDate.parse(date, DatePattern.NORM_DATE_FORMATTER);
        // 如果是今天，直接签到
        if (signDate.isEqual(today)) {
            return doTodaySign(clientUser);
        }
        // 如果是未来日期，抛出异常
        if (signDate.isAfter(today)) {
            throw new ApiException("不能签到未来日期");
        }
        // 校验补签日期
        if (!signDateValidator.isValidResignDate(date)) {
            throw new ApiException("补签日期无效，仅支持补签过去30天内的日期");
        }
        // 检查是否已签到
        if (isSignedOn(date, clientUser)) {
            throw new ApiException("该日期已签到，无需重复补签");
        }
        // 检查剩余补签次数
        int remainingCount = getRemainingResignCount(clientUser);
        if (remainingCount <= 0) {
            throw new ApiException("本月补签次数已用完");
        }
        // 获取签到策略
        SignStrategy strategy = signStrategyFactory.getDefaultStrategy();
        // 构建签到Key
        String signKey = signRedisKeyBuilder.buildMonthlySignPrefixKey(clientUser.getId(), signDate);
        // 获取偏移量
        int offset = signDate.getDayOfMonth() - 1;
        // 执行补签
        stringRedisTemplate.opsForValue().setBit(signKey, offset, true);
        // 更新补签次数
        decrementResignCount(clientUser);
        // 记录补签日志
        log.info("用户{}在{}成功补签日期{}", clientUser.getId(), today, date);
        // 返回连续签到天数
        return strategy.getContinuousSignCount(date, clientUser);
    }

    @Override
    public int getContinuousSignCount(String date, ClientUser clientUser) {
        // 获取有效的签到日期
        String validDate = signDateValidator.getValidSignDate(date);

        SignStrategy strategy = signStrategyFactory.getDefaultStrategy();
        return strategy.getContinuousSignCount(validDate, clientUser);
    }

    @Override
    public int getSignCount(String date, ClientUser clientUser) {
        // 获取有效的签到日期
        String validDate = signDateValidator.getValidSignDate(date);

        SignStrategy strategy = signStrategyFactory.getDefaultStrategy();
        return strategy.getSignCount(validDate, clientUser);
    }

    @Override
    public Map<String, Boolean> getSignInfo(String date, ClientUser clientUser) {
        // 获取有效的签到日期
        String validDate = signDateValidator.getValidSignDate(date);

        SignStrategy strategy = signStrategyFactory.getDefaultStrategy();
        return strategy.getSignInfo(validDate, clientUser);
    }

    /**
     * 检查用户在指定日期是否已签到
     */
    private boolean isSignedOn(String date, ClientUser clientUser) {
        // 解析日期
        LocalDate signDate = LocalDate.parse(date, DatePattern.NORM_DATE_FORMATTER);
        // 构建签到Key
        String signKey = signRedisKeyBuilder.buildMonthlySignPrefixKey(clientUser.getId(), signDate);
        // 获取偏移量
        int offset = signDate.getDayOfMonth() - 1;
        // 查询是否已签到
        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(signKey, offset);
        return Boolean.TRUE.equals(isSigned);
    }

    /**
     * 获取用户可补签的次数
     */
    private int getRemainingResignCount(ClientUser clientUser) {
        // 构建补签计数器Key
        String countKey = signRedisKeyBuilder.buildResignCountPrefixKey(clientUser.getId(), LocalDate.now());
        // 获取已使用的补签次数
        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        int usedCount = countStr == null ? 0 : Integer.parseInt(countStr);

        return Math.max(0, MONTHLY_RESIGN_LIMIT - usedCount);
    }

    /**
     * 减少补签次数
     */
    private void decrementResignCount(ClientUser clientUser) {
        // 构建补签计数器Key
        String countKey = signRedisKeyBuilder.buildResignCountPrefixKey(clientUser.getId(), LocalDate.now());
        // 获取当前计数
        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        int currentCount = countStr == null ? 0 : Integer.parseInt(countStr);
        // 增加计数
        stringRedisTemplate.opsForValue().set(countKey, String.valueOf(currentCount + 1));
        // 如果是本月第一次补签，设置过期时间（到本月底）
        if (currentCount == 0) {
            // 计算到月底的过期时间
            LocalDate now = LocalDate.now();
            LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
            long seconds = now.until(endOfMonth.plusDays(1), java.time.temporal.ChronoUnit.SECONDS);

            stringRedisTemplate.expire(countKey, seconds, TimeUnit.SECONDS);
        }
    }

}
