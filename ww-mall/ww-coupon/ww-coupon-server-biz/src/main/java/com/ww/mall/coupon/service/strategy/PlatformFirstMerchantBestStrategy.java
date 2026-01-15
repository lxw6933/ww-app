package com.ww.mall.coupon.service.strategy;

import cn.hutool.core.util.StrUtil;
import com.ww.mall.coupon.enums.CouponDiscountType;
import com.ww.mall.coupon.enums.ErrorCodeConstants;
import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import com.ww.app.common.exception.ApiException;
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

    /**
     * 按“平台最优 -> 商家最优”的顺序选择默认券，支持用户指定或明确不使用
     */
    @Override
    public SelectionResult select(SelectionContext context) {
        if (context == null) {
            return new SelectionResult(Collections.emptyList(), null);
        }
        List<OrderMemberCouponVO> selectedList = new ArrayList<>();
        List<OrderMemberCouponVO> platformAvailable = context.getPlatformAvailable();
        Comparator<OrderMemberCouponVO> comparator = context.getCouponComparator();
        // 平台券优先，筛掉不可直接用或无优惠收益的券（支持不使用/用户指定平台券）
        OrderMemberCouponVO bestPlatform = null;
        boolean usePlatformCoupon = context.getUsePlatformCoupon() == null || context.getUsePlatformCoupon();
        if (usePlatformCoupon) {
            String selectedPlatformActivityCode = context.getSelectedPlatformActivityCode();
            // 用户选择
            if (StrUtil.isNotBlank(selectedPlatformActivityCode)) {
                OrderMemberCouponVO selected = platformAvailable.stream()
                        .filter(coupon -> selectedPlatformActivityCode.equals(coupon.getActivityCode()))
                        .findFirst()
                        .orElse(null);
                if (selected == null) {
                    throw new ApiException(ErrorCodeConstants.PLATFORM_COUPON_SELECTED_INVALID);
                }
                bestPlatform = isSelectableCoupon(selected) ? selected : null;
            } else {
                // 默认最优
                bestPlatform = pickBestCoupon(filterSelectableCoupons(platformAvailable), comparator);
            }
        }
        List<OrderMemberSmsCouponBO> effectiveOrderBOList = context.getOrderBOList();
        if (bestPlatform != null) {
            selectedList.add(bestPlatform);
            // 平台券均摊后，商家券需基于调整后的订单重新计算
            applyPlatformCouponToOrder(effectiveOrderBOList, bestPlatform);
        }
        CouponBucket merchantBucket = null;
        if (context.getMerchantBucketProvider() != null) {
            merchantBucket = context.getMerchantBucketProvider()
                    .apply(effectiveOrderBOList == null ? Collections.emptyList() : effectiveOrderBOList);
        }
        if (merchantBucket == null) {
            return new SelectionResult(selectedList, null);
        }
        List<OrderMemberCouponVO> merchantAvailable = new ArrayList<>();
        merchantAvailable.addAll(merchantBucket.getAvailableIntegralList());
        merchantAvailable.addAll(merchantBucket.getAvailableCashList());
        boolean useMerchantCoupon = context.getUseMerchantCoupon() == null || context.getUseMerchantCoupon();
        if (!useMerchantCoupon || CollectionUtils.isEmpty(merchantAvailable)) {
            return new SelectionResult(selectedList, merchantBucket);
        }
        Map<Long, List<OrderMemberCouponVO>> merchantCouponMap = new HashMap<>();
        addMerchantCouponsToMap(merchantCouponMap, merchantAvailable);
        Map<Long, String> selectedMerchantActivityCodeMap = context.getSelectedMerchantActivityCodeMap();
        boolean hasSelectedMerchantMap = selectedMerchantActivityCodeMap != null && !selectedMerchantActivityCodeMap.isEmpty();
        for (Map.Entry<Long, List<OrderMemberCouponVO>> entry : merchantCouponMap.entrySet()) {
            // 每个商家选一张最优可用券
            OrderMemberCouponVO bestMerchant;
            Long merchantId = entry.getKey();
            List<OrderMemberCouponVO> merchantCoupons = entry.getValue();
            if (hasSelectedMerchantMap && !selectedMerchantActivityCodeMap.containsKey(merchantId)) {
                continue;
            }
            if (selectedMerchantActivityCodeMap != null && selectedMerchantActivityCodeMap.containsKey(merchantId)) {
                String selectedActivityCode = selectedMerchantActivityCodeMap.get(merchantId);
                if (selectedActivityCode != null && !selectedActivityCode.trim().isEmpty()) {
                    OrderMemberCouponVO selected = merchantCoupons.stream()
                            .filter(coupon -> selectedActivityCode.equals(coupon.getActivityCode()))
                            .findFirst()
                            .orElse(null);
                    if (selected == null) {
                        throw new ApiException(ErrorCodeConstants.MERCHANT_COUPON_SELECTED_INVALID);
                    }
                    bestMerchant = isSelectableCoupon(selected) ? selected : null;
                } else {
                    continue;
                }
            } else {
                bestMerchant = pickBestCoupon(filterSelectableCoupons(merchantCoupons), comparator);
            }
            if (bestMerchant != null) {
                selectedList.add(bestMerchant);
            }
        }
        return new SelectionResult(selectedList, merchantBucket);
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
     * 若积分券与现金券同时存在，则优先使用积分券
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
        // comparator 为降序规则，取 min 才是“最优”
        return targetList.stream().min(comparator).orElse(null);
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
     * 注意：该方法会直接修改传入的 orderBOList
     */
    private void applyPlatformCouponToOrder(List<OrderMemberSmsCouponBO> orderBOList, OrderMemberCouponVO platformCoupon) {
        if (CollectionUtils.isEmpty(orderBOList) || platformCoupon == null) {
            return;
        }
        Map<Long, BigDecimal> allocateMap = platformCoupon.getAllocateResultMap();
        if (allocateMap == null || allocateMap.isEmpty()) {
            return;
        }
        boolean integralType = CouponDiscountType.INTEGRAL_DISCOUNT.equals(platformCoupon.getCouponDiscountType());
        for (OrderMemberSmsCouponBO orderBO : orderBOList) {
            BigDecimal allocated = allocateMap.get(orderBO.getSkuId());
            if (allocated != null) {
                int number = orderBO.getNumber() == null ? 0 : orderBO.getNumber();
                if (integralType) {
                    int lineTotal = (orderBO.getRealIntegral() == null ? 0 : orderBO.getRealIntegral()) * number;
                    int adjustedTotal = Math.max(0, lineTotal - allocated.intValue());
                    orderBO.setRealIntegral(number > 0 ? adjustedTotal / number : 0);
                } else {
                    BigDecimal realAmount = orderBO.getRealAmount() == null ? BigDecimal.ZERO : orderBO.getRealAmount();
                    BigDecimal lineTotal = realAmount.multiply(BigDecimal.valueOf(number));
                    BigDecimal adjustedTotal = lineTotal.subtract(allocated);
                    if (adjustedTotal.compareTo(BigDecimal.ZERO) < 0) {
                        adjustedTotal = BigDecimal.ZERO;
                    }
                    orderBO.setRealAmount(number > 0
                            ? adjustedTotal.divide(BigDecimal.valueOf(number), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
                }
            }
        }
    }

}
