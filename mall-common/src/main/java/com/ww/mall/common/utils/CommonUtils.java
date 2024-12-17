package com.ww.mall.common.utils;

/**
 * @author ww
 * @create 2024-12-17- 15:48
 * @description: 通用工具类
 */
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

}
