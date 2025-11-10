package com.ww.app.member.strategy.sign;

import java.time.LocalDate;

/**
 * @author ww
 * @create 2025-11-10 9:42
 * @description:
 */
public interface ResignStrategy {

    /**
     * 获取不同策略可补签次数
     *
     * @return 补签次数
     */
    int getResignConfig();

    /**
     * 获取补签key剩余过期时间
     */
    long getResignKeyExpireTime();

    /**
     * 构建补签次数key
     */
    String buildResignCountKey(Long userId, LocalDate date);

}
