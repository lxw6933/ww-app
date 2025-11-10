package com.ww.app.member.service.sign;

import com.ww.app.common.common.ClientUser;

import java.time.LocalDate;
import java.util.List;
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
    int doSign(LocalDate date, ClientUser clientUser);

    /**
     * 用户连续签到次数
     *
     * @param date 时间
     * @return 连续签到次数
     */
    int getContinuousSignCount(LocalDate date, ClientUser clientUser);

    /**
     * 用户某月的签到总次数
     *
     * @param date yyyy-MM-dd
     * @return 签到总次数
     */
    int getSignCount(LocalDate date, ClientUser clientUser);

    /**
     * 获取用户指定月份签到详情信息
     *
     * @param date 指定月份 yyyy-MM-dd
     * @return map
     */
    @Deprecated
    Map<LocalDate, Boolean> getSignInfo(LocalDate date, ClientUser clientUser);

    /**
     * 获取当前周期签到详情
     *
     * @param clientUser 用户
     * @return List<Boolean>
     */
    List<Boolean> getSignDetailInfo(ClientUser clientUser);

}
