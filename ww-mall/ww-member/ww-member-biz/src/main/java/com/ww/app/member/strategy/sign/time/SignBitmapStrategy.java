package com.ww.app.member.strategy.sign.time;

import java.time.LocalDate;

/**
 * @author ww
 * @create 2025-10-17 10:14
 * @description:
 */
public interface SignBitmapStrategy {

    /**
     * 获取偏移量
     */
    int getOffset(LocalDate date);

    /**
     * 获取位数
     */
    int getBitCount(LocalDate date);

    /**
     * 根据periodKey获取周期最后一天
     */
    LocalDate getEndDate(String periodKey);

}
