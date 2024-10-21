package com.ww.mall.common.utils;

import cn.hutool.core.math.Money;
import cn.hutool.core.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2024-09-24- 09:09
 * @description: 金额计算工具类
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
     * 计算百分比金额，四舍五入
     *
     * @param price 金额
     * @param rate  百分比，例如说 56.77% 则传入 56.77
     * @return 百分比金额
     */
    public static Integer calculateRatePrice(Integer price, Double rate) {
        return calculateRatePrice(price, rate, 0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 计算百分比金额，向下传入
     *
     * @param price 金额
     * @param rate  百分比，例如说 56.77% 则传入 56.77
     * @return 百分比金额
     */
    public static Integer calculateRatePriceFloor(Integer price, Double rate) {
        return calculateRatePrice(price, rate, 0, RoundingMode.FLOOR).intValue();
    }

    /**
     * 计算百分比金额
     *
     * @param price   金额（单位分）
     * @param count   数量
     * @param percent 折扣（单位分），列如 60.2%，则传入 6020
     * @return 商品总价
     */
    public static Integer calculator(Integer price, Integer count, Integer percent) {
        price = price * count;
        if (percent == null) {
            return price;
        }
        return calculateRatePriceFloor(price, (double) (percent / 100));
    }

    /**
     * 计算百分比金额
     *
     * @param price        金额
     * @param rate         百分比，例如说 56.77% 则传入 56.77
     * @param scale        保留小数位数
     * @param roundingMode 舍入模式
     */
    public static BigDecimal calculateRatePrice(Number price, Number rate, int scale, RoundingMode roundingMode) {
        return NumberUtil.toBigDecimal(price)
                .multiply(NumberUtil.toBigDecimal(rate))
                .divide(BigDecimal.valueOf(100), scale, roundingMode);
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
     * <p>
     * 例如说 fen 为 1 时，则结果为 0.01
     *
     * @param fen 分
     * @return 元
     */
    public static String fenToYuanStr(int fen) {
        return new Money(0, fen).toString();
    }

    /**
     * 金额相乘，默认进行四舍五入
     * <p>
     * 位数：{@link #PRICE_SCALE}
     *
     * @param price 金额
     * @param count 数量
     * @return 金额相乘结果
     */
    public static BigDecimal priceMultiply(BigDecimal price, BigDecimal count) {
        if (price == null || count == null) {
            return null;
        }
        return price.multiply(count).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 金额相乘（百分比），默认进行四舍五入
     * 位数：{@link #PRICE_SCALE}
     *
     * @param price   金额
     * @param percent 百分比
     * @return 金额相乘结果
     */
    public static BigDecimal priceMultiplyPercent(BigDecimal price, BigDecimal percent) {
        if (price == null || percent == null) {
            return null;
        }
        return price.multiply(percent).divide(PERCENT_100, PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 按比例将优惠金额均摊到 BO 集合
     *
     * @param boList   BO 集合
     * @param discount 总优惠金额
     * @return 返回每个 BO 的优惠金额映射
     */
    public static <T> Map<T, BigDecimal> allocateDiscount(List<MoneyBO<T>> boList, BigDecimal discount) {
        Map<T, BigDecimal> result = new HashMap<>();

        // 1. 计算总价
        BigDecimal totalAmount = boList.stream()
                .map(MoneyBO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 初始化变量，累积分摊的优惠金额，确保精度不丢失
        BigDecimal remainingDiscount = discount;

        // 3. 遍历 BO 集合，按比例计算每个 BO 的优惠金额
        for (int i = 0; i < boList.size(); i++) {
            MoneyBO<T> bo = boList.get(i);

            // 计算当前 BO 应分摊的优惠金额
            BigDecimal proportion = bo.getPrice().divide(totalAmount, 8, RoundingMode.HALF_UP);
            BigDecimal allocatedDiscount = discount.multiply(proportion).setScale(PRICE_SCALE, RoundingMode.HALF_UP);

            // 如果是最后一个 BO，直接将剩余的优惠金额分摊给它，确保总金额精度不丢失
            if (i == boList.size() - 1) {
                allocatedDiscount = remainingDiscount;
            }

            result.put(bo.getId(), allocatedDiscount);

            // 减少剩余的优惠金额
            remainingDiscount = remainingDiscount.subtract(allocatedDiscount);
        }

        return result;
    }

    public static void main(String[] args) {
        // 示例数据
        List<MoneyBO<String>> skuList = Arrays.asList(
                new MoneyBO<>("sku1", new BigDecimal("0.01")),
                new MoneyBO<>("sku2", new BigDecimal("200.00")),
                new MoneyBO<>("sku3", new BigDecimal("301.00"))
        );

        BigDecimal discount = new BigDecimal("50.00");

        // 计算并打印优惠金额分摊结果
        Map<String, BigDecimal> result = allocateDiscount(skuList, discount);
        result.forEach((skuId, allocated) ->
                System.out.println("SKU: " + skuId + ", 分摊优惠金额: " + allocated));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MoneyBO<T> {

        private T id;

        private BigDecimal price;

    }

}
