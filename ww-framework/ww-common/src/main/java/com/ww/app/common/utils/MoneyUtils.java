package com.ww.app.common.utils;

import cn.hutool.core.math.Money;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2024-09-24- 09:09
 * @description: 金额计算工具类 提供金额计算、转换、分摊等功能
 */
public class MoneyUtils {

    /**
     * 金额的小数位数
     */
    private static final int PRICE_SCALE = 2;

    /**
     * 百分比对应的 BigDecimal 对象
     */
    public static final BigDecimal PERCENT_100 = BigDecimal.valueOf(100);

    /**
     * 默认拆分最小金额
     */
    private static final BigDecimal DEFAULT_MIN_AMOUNT = new BigDecimal("0.01");

    /**
     * 默认舍入模式
     */
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 红包拆分时的随机范围倍数
     */
    private static final BigDecimal RED_PACKET_RANGE_MULTIPLIER = BigDecimal.valueOf(2);

    private MoneyUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 计算百分比金额，四舍五入
     *
     * @param price 金额
     * @param rate  百分比，例如说 56.77% 则传入 56.77
     * @return 百分比金额
     */
    public static Integer calculateRatePrice(Integer price, Double rate) {
        return calculateRatePrice(price, rate, 0, DEFAULT_ROUNDING_MODE).intValue();
    }

    /**
     * 计算百分比金额，向下取整
     *
     * @param price 金额
     * @param rate  百分比，例如说 56.77% 则传入 56.77
     * @return 百分比金额
     */
    public static Integer calculateRatePriceFloor(Integer price, Double rate) {
        return calculateRatePrice(price, rate, 0, RoundingMode.FLOOR).intValue();
    }

    /**
     * 计算商品总价（含折扣）
     *
     * @param price    单价（单位分）
     * @param count    数量
     * @param percent  折扣（单位分），例如 60.2%，则传入 6020
     * @return 商品总价
     */
    public static Integer calculator(Integer price, Integer count, Integer percent) {
        if (price == null || count == null) {
            return null;
        }
        int totalPrice = price * count;
        if (percent == null) {
            return totalPrice;
        }
        return calculateRatePriceFloor(totalPrice, (double) (percent / 100));
    }

    /**
     * 计算百分比金额
     *
     * @param price        金额
     * @param rate         百分比，例如说 56.77% 则传入 56.77
     * @param scale        保留小数位数
     * @param roundingMode 舍入模式
     * @return 计算结果
     */
    public static BigDecimal calculateRatePrice(Number price, Number rate, int scale, RoundingMode roundingMode) {
        if (price == null || rate == null) {
            return null;
        }
        return NumberUtil.toBigDecimal(price)
                .multiply(NumberUtil.toBigDecimal(rate))
                .divide(PERCENT_100, scale, roundingMode);
    }

    /**
     * 分转元
     *
     * @param fen 分
     * @return 元
     */
    public static BigDecimal fenToYuan(int fen) {
        return new Money(0, fen).getAmount();
    }

    /**
     * 分转元（字符串）
     *
     * @param fen 分
     * @return 元
     */
    public static String fenToYuanStr(int fen) {
        return new Money(0, fen).toString();
    }

    /**
     * 金额相乘，默认进行四舍五入
     *
     * @param price 金额
     * @param count 数量
     * @return 金额相乘结果
     */
    public static BigDecimal priceMultiply(BigDecimal price, BigDecimal count) {
        if (price == null || count == null) {
            return null;
        }
        return price.multiply(count).setScale(PRICE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 金额相乘（百分比），默认进行四舍五入
     *
     * @param price   金额
     * @param percent 百分比
     * @return 金额相乘结果
     */
    public static BigDecimal priceMultiplyPercent(BigDecimal price, BigDecimal percent) {
        if (price == null || percent == null) {
            return null;
        }
        return price.multiply(percent).divide(PERCENT_100, PRICE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 按比例将优惠金额均摊到 BO 集合
     *
     * @param boList       BO 集合
     * @param discount     总优惠金额
     * @param scale        金额的小数位数
     * @param roundingMode 金额的舍入模式
     * @return 返回每个 BO 的优惠金额映射
     */
    public static <T> Map<T, BigDecimal> allocateDiscount(List<MoneyBO<T>> boList, BigDecimal discount, int scale, RoundingMode roundingMode) {
        if (CollectionUtils.isEmpty(boList)) {
            throw new IllegalArgumentException("boList 不能为空");
        }
        
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("优惠金额不能为负数");
        }
        
        Map<T, BigDecimal> result = new HashMap<>();
        
        // 过滤掉价格为0的商品，避免除零异常
        List<MoneyBO<T>> validBoList = boList.stream()
                .filter(bo -> bo.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        // 处理价格为0的商品，直接放入结果集
        boList.stream()
                .filter(bo -> bo.getPrice().compareTo(BigDecimal.ZERO) == 0)
                .forEach(bo -> result.put(bo.getId(), BigDecimal.ZERO));
        
        if (validBoList.isEmpty()) {
            return result;
        }
        
        // 计算总价
        BigDecimal totalAmount = validBoList.stream()
                .map(MoneyBO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 如果优惠金额大于总金额，则限制为总金额
        if (totalAmount.compareTo(discount) < 0) {
            discount = totalAmount;
        }
        
        // 初始化变量，累积分摊的优惠金额
        BigDecimal remainingDiscount = discount;
        
        // 遍历有效商品列表，按比例计算每个商品的优惠金额
        for (int i = 0; i < validBoList.size(); i++) {
            MoneyBO<T> bo = validBoList.get(i);
            
            // 计算当前商品应分摊的优惠金额
            BigDecimal proportion = bo.getPrice().divide(totalAmount, 8, roundingMode);
            BigDecimal allocatedDiscount = discount.multiply(proportion).setScale(scale, roundingMode);
            
            // 确保分摊的优惠金额不超过商品自身价格
            if (allocatedDiscount.compareTo(bo.getPrice()) > 0) {
                allocatedDiscount = bo.getPrice();
            }
            
            // 如果是最后一个商品，将剩余的优惠金额分摊给它
            if (i == validBoList.size() - 1) {
                allocatedDiscount = remainingDiscount.compareTo(bo.getPrice()) > 0 ? 
                        bo.getPrice() : remainingDiscount;
            }
            
            result.put(bo.getId(), allocatedDiscount);
            remainingDiscount = remainingDiscount.subtract(allocatedDiscount);
        }
        
        return result;
    }

    /**
     * 分摊金额的简化方法（默认小数位数和舍入模式）
     */
    public static <T> Map<T, BigDecimal> allocateBigDecimalDiscount(List<MoneyBO<T>> itemList, BigDecimal discount) {
        return allocateDiscount(itemList, discount, PRICE_SCALE, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 分摊金额的简化方法（支持 int 类型的总金额）
     */
    public static <T> Map<T, BigDecimal> allocateIntDiscount(List<MoneyBO<T>> itemList, int discount) {
        return allocateDiscount(itemList, new BigDecimal(discount), 0, DEFAULT_ROUNDING_MODE);
    }

    /**
     * 拆分红包
     *
     * @param totalAmount 红包总金额，单位为元
     * @param minAmount   最小红包金额
     * @param totalCount  拆分的红包个数
     * @return 拆分后的红包金额列表
     */
    public static List<BigDecimal> splitRedPacket(BigDecimal totalAmount, BigDecimal minAmount, int totalCount) {
        validateRedPacketParams(totalAmount, minAmount, totalCount);

        List<BigDecimal> redPackets = new ArrayList<>(totalCount);
        BigDecimal remainingAmount = totalAmount;

        // 计算每个红包可分配金额的均值
        BigDecimal avgAmount = remainingAmount.divide(BigDecimal.valueOf(totalCount), PRICE_SCALE, RoundingMode.DOWN);
        
        // 分配红包
        for (int i = 0; i < totalCount - 1; i++) {
            BigDecimal redPacketAmount = calculateRedPacketAmount(avgAmount, minAmount);
            redPackets.add(redPacketAmount);
            remainingAmount = remainingAmount.subtract(redPacketAmount);
            avgAmount = remainingAmount.divide(BigDecimal.valueOf(totalCount - i - 1), PRICE_SCALE, RoundingMode.DOWN);
        }
        
        // 最后一个红包，剩余金额全部分配
        redPackets.add(remainingAmount);
        
        return redPackets;
    }

    /**
     * 使用默认最小金额拆分红包
     */
    public static List<BigDecimal> splitRedPacket(BigDecimal totalAmount, int totalCount) {
        return splitRedPacket(totalAmount, DEFAULT_MIN_AMOUNT, totalCount);
    }

    /**
     * 验证红包拆分参数
     */
    private static void validateRedPacketParams(BigDecimal totalAmount, BigDecimal minAmount, int totalCount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0 || totalCount <= 0) {
            throw new IllegalArgumentException("总金额和数量必须大于0");
        }
        if (minAmount == null || minAmount.compareTo(DEFAULT_MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("最低金额不能小于" + DEFAULT_MIN_AMOUNT);
        }
        BigDecimal minTotalAmount = minAmount.multiply(BigDecimal.valueOf(totalCount));
        if (totalAmount.compareTo(minTotalAmount) < 0) {
            throw new IllegalArgumentException("总金额不能小于" + minTotalAmount);
        }
    }

    /**
     * 计算单个红包金额
     */
    private static BigDecimal calculateRedPacketAmount(BigDecimal avgAmount, BigDecimal minAmount) {
        if (avgAmount.compareTo(minAmount) <= 0) {
            return minAmount;
        }
        
        BigDecimal maxAmount = avgAmount.subtract(minAmount)
                .multiply(RED_PACKET_RANGE_MULTIPLIER)
                .add(minAmount);
                
        if (maxAmount.compareTo(minAmount) <= 0) {
            return minAmount;
        }
        
        double randomDouble = RandomUtil.randomDouble(minAmount.doubleValue(), maxAmount.doubleValue());
        return BigDecimal.valueOf(randomDouble).setScale(PRICE_SCALE, RoundingMode.DOWN);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MoneyBO<T> {
        private T id;
        private BigDecimal price;
    }
}
