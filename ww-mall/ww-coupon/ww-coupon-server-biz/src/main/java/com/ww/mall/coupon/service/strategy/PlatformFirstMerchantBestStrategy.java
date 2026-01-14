package com.ww.mall.coupon.service.strategy;

import cn.hutool.core.map.MapUtil;
import com.ww.mall.coupon.enums.CouponDiscountType;
import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认选券策略：平台券最优 + 各商家最优
 */
@Component
public class PlatformFirstMerchantBestStrategy implements DefaultCouponSelectStrategy {

    @Override
    public List<OrderMemberCouponVO> select(SelectionContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        List<OrderMemberCouponVO> selectedList = new ArrayList<>();
        List<OrderMemberCouponVO> platformAvailable = context.getPlatformAvailable();
        Comparator<OrderMemberCouponVO> comparator = context.getCouponComparator();
        // 平台券优先，筛掉不可直接用或无优惠收益的券
        OrderMemberCouponVO bestPlatform = pickBestCoupon(filterSelectableCoupons(platformAvailable), comparator);
        List<OrderMemberSmsCouponBO> effectiveOrderBOList = context.getOrderBOList();
        if (bestPlatform != null) {
            selectedList.add(bestPlatform);
            // 平台券均摊后，商家券需基于调整后的订单重新计算
            effectiveOrderBOList = applyPlatformCouponToOrder(effectiveOrderBOList, bestPlatform);
        }
        if (context.getMerchantAvailableProvider() == null) {
            return selectedList;
        }
        List<OrderMemberCouponVO> merchantAvailable = context.getMerchantAvailableProvider()
                .apply(effectiveOrderBOList == null ? Collections.emptyList() : effectiveOrderBOList);
        if (CollectionUtils.isEmpty(merchantAvailable)) {
            return selectedList;
        }
        Map<Long, List<OrderMemberCouponVO>> merchantCouponMap = new HashMap<>();
        addMerchantCouponsToMap(merchantCouponMap, merchantAvailable);
        for (Map.Entry<Long, List<OrderMemberCouponVO>> entry : merchantCouponMap.entrySet()) {
            // 每个商家选一张最优可用券
            OrderMemberCouponVO bestMerchant = pickBestCoupon(filterSelectableCoupons(entry.getValue()), comparator);
            if (bestMerchant != null) {
                selectedList.add(bestMerchant);
            }
        }
        return selectedList;
    }

    /**
     * 按商家分组商家券
     */
    private void addMerchantCouponsToMap(Map<Long, List<OrderMemberCouponVO>> merchantCouponMap,
                                         List<OrderMemberCouponVO> couponList) {
        for (OrderMemberCouponVO coupon : couponList) {
            if (coupon.getMerchantId() == null) {
                continue;
            }
            merchantCouponMap.computeIfAbsent(coupon.getMerchantId(), key -> new ArrayList<>()).add(coupon);
        }
    }

    /**
     * 获取列表中最优优惠券
     */
    private OrderMemberCouponVO pickBestCoupon(List<OrderMemberCouponVO> couponList,
                                               Comparator<OrderMemberCouponVO> comparator) {
        if (CollectionUtils.isEmpty(couponList) || comparator == null) {
            return null;
        }
        List<OrderMemberCouponVO> targetList = couponList;
        boolean hasIntegral = couponList.stream()
                .anyMatch(coupon -> CouponDiscountType.INTEGRAL_DISCOUNT.equals(coupon.getCouponDiscountType()));
        boolean hasCash = couponList.stream()
                .anyMatch(coupon -> !CouponDiscountType.INTEGRAL_DISCOUNT.equals(coupon.getCouponDiscountType()));
        if (hasIntegral && hasCash) {
            targetList = couponList.stream()
                    .filter(coupon -> CouponDiscountType.INTEGRAL_DISCOUNT.equals(coupon.getCouponDiscountType()))
                    .collect(Collectors.toList());
        }
        return targetList.stream().max(comparator).orElse(null);
    }

    /**
     * 过滤出可直接使用且有优惠收益的券
     */
    private List<OrderMemberCouponVO> filterSelectableCoupons(List<OrderMemberCouponVO> couponList) {
        if (CollectionUtils.isEmpty(couponList)) {
            return Collections.emptyList();
        }
        List<OrderMemberCouponVO> result = new ArrayList<>();
        for (OrderMemberCouponVO coupon : couponList) {
            if (isSelectableCoupon(coupon)) {
                result.add(coupon);
            }
        }
        return result;
    }

    /**
     * 判断优惠券是否可直接选中
     */
    private boolean isSelectableCoupon(OrderMemberCouponVO coupon) {
        if (coupon == null || coupon.getDisabled() != null) {
            return false;
        }
        BigDecimal discountTotalAmount = coupon.getDiscountTotalAmount();
        BigDecimal lackAmount = coupon.getLackAmount();
        boolean hasBenefit = discountTotalAmount != null && discountTotalAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean noLack = lackAmount == null || lackAmount.compareTo(BigDecimal.ZERO) <= 0;
        return hasBenefit && noLack;
    }

    /**
     * 按平台券均摊结果重算订单行的实付金额/积分（用于商家券门槛判断）
     */
    private List<OrderMemberSmsCouponBO> applyPlatformCouponToOrder(List<OrderMemberSmsCouponBO> orderBOList,
                                                                    OrderMemberCouponVO platformCoupon) {
        if (CollectionUtils.isEmpty(orderBOList) || platformCoupon == null) {
            return orderBOList == null ? Collections.emptyList() : orderBOList;
        }
        Map<Long, BigDecimal> allocateMap = platformCoupon.getAllocateResultMap();
        if (MapUtil.isEmpty(allocateMap)) {
            return orderBOList;
        }
        boolean integralType = CouponDiscountType.INTEGRAL_DISCOUNT.equals(platformCoupon.getCouponDiscountType());
        List<OrderMemberSmsCouponBO> adjustedList = new ArrayList<>(orderBOList.size());
        for (OrderMemberSmsCouponBO orderBO : orderBOList) {
            OrderMemberSmsCouponBO copy = copyOrderMemberSmsCouponBO(orderBO);
            BigDecimal allocated = allocateMap.get(orderBO.getSkuId());
            if (allocated != null) {
                int number = copy.getNumber() == null ? 0 : copy.getNumber();
                if (integralType) {
                    int lineTotal = (copy.getRealIntegral() == null ? 0 : copy.getRealIntegral()) * number;
                    int adjustedTotal = Math.max(0, lineTotal - allocated.intValue());
                    copy.setRealIntegral(number > 0 ? adjustedTotal / number : 0);
                } else {
                    BigDecimal realAmount = copy.getRealAmount() == null ? BigDecimal.ZERO : copy.getRealAmount();
                    BigDecimal lineTotal = realAmount.multiply(BigDecimal.valueOf(number));
                    BigDecimal adjustedTotal = lineTotal.subtract(allocated);
                    if (adjustedTotal.compareTo(BigDecimal.ZERO) < 0) {
                        adjustedTotal = BigDecimal.ZERO;
                    }
                    copy.setRealAmount(number > 0
                            ? adjustedTotal.divide(BigDecimal.valueOf(number), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
                }
            }
            adjustedList.add(copy);
        }
        return adjustedList;
    }

    /**
     * 复制订单行数据，避免改写原始参数
     */
    private OrderMemberSmsCouponBO copyOrderMemberSmsCouponBO(OrderMemberSmsCouponBO source) {
        OrderMemberSmsCouponBO copy = new OrderMemberSmsCouponBO();
        copy.setMerchantId(source.getMerchantId());
        copy.setSmsId(source.getSmsId());
        copy.setSpuId(source.getSpuId());
        copy.setSkuId(source.getSkuId());
        copy.setCategoryId(source.getCategoryId());
        copy.setBrandId(source.getBrandId());
        copy.setNumber(source.getNumber());
        copy.setRealAmount(source.getRealAmount());
        copy.setRealIntegral(source.getRealIntegral());
        copy.setActivityUseCoupon(source.isActivityUseCoupon());
        return copy;
    }
}
