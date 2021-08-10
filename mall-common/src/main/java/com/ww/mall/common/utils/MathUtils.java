package com.ww.mall.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: Math Utils
 * @author: ww
 * @create: 2021-06-01 14:37
 */
public class MathUtils {

    private MathUtils() {
    }

    /**
     * 计算方差
     *
     * @param numbers 数据
     * @param scale   保留位数
     * @return 方差差
     */
    public static BigDecimal getAverage(List<BigDecimal> numbers, int scale) {
        if (CollectionUtils.isEmpty(numbers)) {
            return BigDecimal.ZERO;
        }
        // 第一步：计算数据个数总和
        BigDecimal size = new BigDecimal(numbers.size());
        BigDecimal sum = numbers.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 第二步：计算平均值
        return sum.divide(size, scale, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * 计算方差
     *
     * @param numbers 数据
     * @param scale   保留位数
     * @return 方差差
     */
    public static BigDecimal getVariance(List<BigDecimal> numbers, int scale) {
        if (CollectionUtils.isEmpty(numbers)) {
            return BigDecimal.ZERO;
        }
        // 第二步：计算平均值
        BigDecimal avg = getAverage(numbers, scale);
        // 第一步：计算数据个数总和
        BigDecimal size = new BigDecimal(numbers.size());

        // 第三步：标准差 = (每个数据 - 平均值)平方之和，再除以长度
        BigDecimal num = numbers.stream()
                .map(number -> number.subtract(avg).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(size, scale, BigDecimal.ROUND_HALF_EVEN);
        return new BigDecimal(Double.toString(num.doubleValue()))
                .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * 计算样本标准差（标准差定义为方差的算术平方根，反映组内个体间的离散程度）
     *
     * @param numbers 数据
     * @param scale   保留位数
     * @return 标准差
     */
    public static BigDecimal getStandardDeviation2(List<BigDecimal> numbers, int scale) {
        if (CollectionUtils.isEmpty(numbers)) {
            return BigDecimal.ZERO;
        }
        // 第一步：计算数据个数总和
        BigDecimal size = new BigDecimal(numbers.size());
        // 第二步：计算平均值
        BigDecimal avg = getAverage(numbers, scale);
        // 第三步：标准差 = (每个数据 - 平均值)平方之和，再除以长度
        BigDecimal num = numbers.stream()
                .map(number -> number.subtract(avg).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(size.subtract(BigDecimal.ONE), scale, BigDecimal.ROUND_HALF_EVEN);
        return new BigDecimal(Double.toString(Math.sqrt(num.doubleValue())))
                .setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * 计算总体标准差
     *
     * @param numbers 数据
     * @param scale   保留位数
     * @return 标准差
     */
    public static BigDecimal getStandardDeviation(List<BigDecimal> numbers, int scale) {
        BigDecimal variance = getVariance(numbers, scale);
        return new BigDecimal(Double.toString(Math.sqrt(variance.doubleValue())))
                .setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    /**
     * 获取波峰和波谷（波峰比附近的值都大，波谷比附近的值都小）
     *
     * @param numbers 数据集合
     * @return List<Integer>
     */
    public static List<WaveData> getWave(List<BigDecimal> numbers) {
        // 波峰波谷小标，波峰为正数，波谷为负数
        List<WaveData> list = new ArrayList<>();
        int size = numbers.size();
        if (size < 2) {
            return list;
        }
        // -1 ：（direction ---> 波峰）   1：（direction ---> 波谷）
        // 获取第一个值，如果大于零，说明下一个出现的是波峰，设置当前方向为： 波谷 ---> 波峰
        int direction = numbers.get(0).compareTo(BigDecimal.ZERO) > 0 ? -1 : 1;
        for (int i = 0; i < size - 1; i++) {
            // 1：如果当前方向是走向波谷 只有下一个值大于上一个值，说明上一个值是波谷，否则继续
            // 2：如果当前方向是走向波峰 只有下一个值小于上一个值，说明上一个值是波峰，否则继续
            BigDecimal num = numbers.get(i + 1)
                    .subtract(numbers.get(i))
                    .multiply(new BigDecimal(direction));
            if (num.compareTo(BigDecimal.ZERO) > 0) {
                // （获取到一个波峰或波谷）方向取反获取相对的方向
                direction = direction * -1;
                if (direction == 1) {
                    // 波峰
                    list.add(new WaveData(true, i));
                } else {
                    // 波谷
                    list.add(new WaveData(false, i));
                }
            }
        }
        return list;
    }


    /**
     * 波峰和波谷
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaveData {

        /**
         * true：波峰
         * false：波谷
         */
        private boolean up;

        /**
         * 数据下标
         */
        private int idx;

    }

}
