package com.ww.mall.member.service;

import com.ww.mall.common.common.MallClientUser;

import java.util.Map;

/**
 * @author ww
 * @create 2023-07-21- 09:16
 * @description:
 */
public interface SignService {

    /**
     * 用户签到
     *
     * @param date 签到日期
     * @return int
     */
    int doSign(String date, MallClientUser clientUser);

    /**
     * 用户连续签到次数
     *
     * @param date 时间
     * @return 连续签到次数
     */
    int getContinuousSignCount(String date, MallClientUser clientUser);

    /**
     * 用户某月的签到总次数
     *
     * @param date yyyy-MM-dd
     * @return 签到总次数
     */
    int getSignCount(String date, MallClientUser clientUser);

    /**
     * 获取用户指定月份签到详情信息
     *
     * @param date 指定月份 yyyy-MM-dd
     * @return map
     */
    Map<String, Boolean> getSignInfo(String date, MallClientUser clientUser);

}
