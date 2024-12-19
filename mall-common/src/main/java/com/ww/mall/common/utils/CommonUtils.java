package com.ww.mall.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-17- 15:48
 * @description: 通用工具类
 */
@Slf4j
public class CommonUtils {

    private CommonUtils() {}

    /**
     * 获取循环次数
     *
     * @param total 总数量
     * @param batchNum 批次处理数量
     * @return 循环次数
     */
    public static int getCircleNumber(int total, int batchNum) {
        return (total + batchNum - 1) / batchNum;
    }

    /**
     * 获取列表中最后一条记录的游标字段值
     *
     * @param records     数据记录列表
     * @param cursorField 游标字段名称
     * @param <T>         实体类型
     * @return 游标值
     */
    public static <T> Object getCursorValue(List<T> records, String cursorField) {
        try {
            if (!records.isEmpty()) {
                T lastRecord = records.get(records.size() - 1);
                return lastRecord.getClass().getDeclaredField(cursorField).get(lastRecord);
            }
        } catch (Exception e) {
            log.error("Error retrieving cursor value: {}", e.getMessage());
        }
        return null;
    }

}
