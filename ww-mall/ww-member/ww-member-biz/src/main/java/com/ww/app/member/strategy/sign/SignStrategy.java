package com.ww.app.member.strategy.sign;

import com.ww.app.common.common.ClientUser;
import com.ww.app.member.enums.SignPeriodEnum;

import java.util.Map;

/**
 * @author ww
 * @create 2023-07-21- 09:16
 * @description: 签到策略接口
 */
public interface SignStrategy {

    /**
     * 执行签到
     *
     * @param date       日期
     * @param clientUser 用户信息
     * @return 签到结果
     */
    int doSign(String date, ClientUser clientUser);

    /**
     * 获取连续签到次数
     *
     * @param date       日期
     * @param clientUser 用户信息
     * @return 连续签到次数
     */
    int getContinuousSignCount(String date, ClientUser clientUser);

    /**
     * 获取签到总次数
     *
     * @param date       日期
     * @param clientUser 用户信息
     * @return 签到总次数
     */
    int getSignCount(String date, ClientUser clientUser);

    /**
     * 获取签到详情
     *
     * @param date       日期
     * @param clientUser 用户信息
     * @return 签到详情
     */
    Map<String, Boolean> getSignInfo(String date, ClientUser clientUser);

    /**
     * 获取策略类型
     *
     * @return 策略类型
     */
    SignPeriodEnum getType();
} 