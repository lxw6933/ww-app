package com.ww.app.common.utils;

import cn.hutool.core.math.Money;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.ww.app.common.exception.ApiException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
     * 默认拆分最小金额
     */
    private static final BigDecimal DEFAULT_MIN_AMOUNT = new BigDecimal("0.01");

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
        Map<T, BigDecimal> result = new HashMap<>();
        // 1. 计算总价
        BigDecimal totalAmount = boList.stream()
                .map(MoneyBO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            // 总金额为0，不进行均摊
            for (MoneyBO<T> bo : boList) {
                result.put(bo.getId(), BigDecimal.ZERO);
            }
            return result;
        }
        if (totalAmount.compareTo(discount) < 0) {
            discount = totalAmount;
        }
        // 2. 初始化变量，累积分摊的优惠金额，确保精度不丢失
        BigDecimal remainingDiscount = discount;
        // 3. 遍历 BO 集合，按比例计算每个 BO 的优惠金额
        for (int i = 0; i < boList.size(); i++) {
            MoneyBO<T> bo = boList.get(i);
            // 计算当前 BO 应分摊的优惠金额
            BigDecimal proportion = bo.getPrice().divide(totalAmount, 8, roundingMode);
            BigDecimal allocatedDiscount = discount.multiply(proportion).setScale(scale, roundingMode);
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

    /**
     * 分摊金额的简化方法（默认小数位数和舍入模式）
     */
    public static <T> Map<T, BigDecimal> allocateBigDecimalDiscount(List<MoneyBO<T>> itemList, BigDecimal discount) {
        return allocateDiscount(itemList, discount, 2, RoundingMode.HALF_UP);
    }

    /**
     * 分摊金额的简化方法（支持 int 类型的总金额）
     */
    public static <T> Map<T, BigDecimal> allocateIntDiscount(List<MoneyBO<T>> itemList, int discount) {
        return allocateDiscount(itemList, new BigDecimal(discount), 0, RoundingMode.HALF_UP);
    }

    /**
     * 拆分红包
     *
     * @param totalAmount 红包总金额，单位为元
     * @param totalCount  拆分的红包个数
     * @return 拆分后的红包金额列表
     */
    public static List<BigDecimal> splitRedPacket(BigDecimal totalAmount, BigDecimal minAmount, int totalCount) {
        // 校验输入数据是否合法
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0 || totalCount <= 0) {
            throw new IllegalArgumentException("总金额和数量必须大于0");
        }
        if (minAmount.compareTo(DEFAULT_MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("最低金额不能小于" + DEFAULT_MIN_AMOUNT);
        }
        BigDecimal minTotalAmount = minAmount.multiply(BigDecimal.valueOf(totalCount));
        if (totalAmount.compareTo(minTotalAmount) < 0) {
            throw new IllegalArgumentException("总金额不能小于" + minTotalAmount);
        }

        List<BigDecimal> redPackets = new ArrayList<>();

        // 可分配的金额
        BigDecimal allocateAmount = totalAmount;

        // 第一步：计算每个红包可分配金额的均值
        BigDecimal avgAllocateAmount = allocateAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.DOWN);
        // 第二步：分配剩余金额
        BigDecimal range = BigDecimal.valueOf(2);
        for (int i = 0; i < totalCount - 1; i++) {
//            boolean isAllocate = allocateAmount.compareTo(BigDecimal.ZERO) > 0;
            boolean isAllocate = avgAllocateAmount.compareTo(minAmount) > 0;
            // 红包分配金额
            BigDecimal redPackageAllocateAmount = minAmount;
            // 存在可分配金额则进行随机取值，不存在则minAmount
            if (isAllocate) {
                // 红包可分配最大金额
                BigDecimal maxAllocateAmount = avgAllocateAmount.subtract(minAmount).multiply(range).add(minAmount);
                if (maxAllocateAmount.compareTo(minAmount) != 0) {
                    // 在[0, maxAllocateAmount]随机生成红包分配金额
                    double randomDouble = RandomUtil.randomDouble(minAmount.doubleValue(), maxAllocateAmount.doubleValue());
                    redPackageAllocateAmount = BigDecimal.valueOf(randomDouble).setScale(2, RoundingMode.DOWN);
                }
            }

            redPackets.add(redPackageAllocateAmount);
            // 重新计算可分配金额
            allocateAmount = allocateAmount.subtract(redPackageAllocateAmount);
            // 重新计算可分配的平均金额
            avgAllocateAmount = allocateAmount.divide(BigDecimal.valueOf(totalCount - i + 1), 2, RoundingMode.DOWN);
        }
        // 最后一个红包，剩余金额全部分配给最后一个红包
        redPackets.add(allocateAmount);
        return redPackets;
    }

    public static List<BigDecimal> splitRedPacket(BigDecimal totalAmount, int totalCount) {
        return splitRedPacket(totalAmount, DEFAULT_MIN_AMOUNT, totalCount);
    }

    public static void main(String[] args) {
        testAllocateDiscount();
//        testSplitRedPacket();
        for (int i = 0; i < 1; i++) {
//            testSplitRedPacket();
        }
    }

    private static void testSplitRedPacket() {
        BigDecimal min = new BigDecimal("5");
//        BigDecimal min = DEFAULT_MIN_AMOUNT;
//        BigDecimal totalAmount = new BigDecimal("4.02");
        BigDecimal totalAmount = new BigDecimal("100.25");
//        int count = 10;
        int count = 2;

//        List<BigDecimal> redPackets = splitRedPacket(totalAmount, count);
        List<BigDecimal> redPackets = splitRedPacket(totalAmount, min, count);
        BigDecimal issueTotalAmount = BigDecimal.ZERO;
        for (BigDecimal redPacket : redPackets) {
            if (redPacket.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException("红包金额异常" + redPacket);
            }
            if (redPacket.compareTo(min) < 0) {
                throw new ApiException("红包金额低于最小金额" + min);
            }
            System.out.println("红包金额: " + redPacket);
            issueTotalAmount = issueTotalAmount.add(redPacket);
        }
        if (totalAmount.compareTo(issueTotalAmount) != 0) {
            throw new ApiException("超发金额");
        }
//        System.out.println("发放红包总金额" + issueTotalAmount);
    }

    private static void testAllocateDiscount() {
        // 示例数据
        List<MoneyBO<String>> skuList = Arrays.asList(
//                new MoneyBO<>("sku1", new BigDecimal("0.01")),
//                new MoneyBO<>("sku2", new BigDecimal("200.00")),
//                new MoneyBO<>("sku3", new BigDecimal("301.00"))
                new MoneyBO<>("sku1", new BigDecimal("1")),
                new MoneyBO<>("sku2", new BigDecimal("4")),
                new MoneyBO<>("sku3", new BigDecimal("1"))
        );
        int discount = 2;
//        BigDecimal discount = new BigDecimal("500");
        // 计算并打印优惠金额分摊结果
        Map<String, BigDecimal> result = allocateIntDiscount(skuList, discount);
//        Map<String, BigDecimal> result = allocateBigDecimalDiscount(skuList, discount);
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
