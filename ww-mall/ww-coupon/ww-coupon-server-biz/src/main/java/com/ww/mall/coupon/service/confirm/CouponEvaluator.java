package com.ww.mall.coupon.service.confirm;

import com.ww.app.common.utils.MoneyUtils;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.convert.CouponConvert;
import com.ww.mall.coupon.enums.ApplyProductRangeType;
import com.ww.mall.coupon.enums.CouponDiscountType;
import com.ww.mall.coupon.enums.CouponType;
import com.ww.mall.coupon.service.strategy.CouponBucket;
import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.MemberCouponCenterVO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.filterList;

@Getter
@Component
public class CouponEvaluator {

    /**
     *  提供统一的优惠券排序规则（用于最优券选择）
     */
    private final Comparator<OrderMemberCouponVO> couponComparator = Comparator
            .comparing(OrderMemberCouponVO::getDiscountTotalAmount).reversed()
            .thenComparing(OrderMemberCouponVO::getUseEndTime)
            .thenComparing(OrderMemberCouponVO::getLackAmount);

    /**
     * 基于订单商品信息计算优惠券可用/不可用分桶
     */
    public CouponBucket buildBucket(List<MemberCouponCenterVO> memberCouponList, List<OrderMemberSmsCouponBO> orderBOList) {
        CouponBucket bucket = new CouponBucket();
        if (CollectionUtils.isEmpty(memberCouponList)) {
            return bucket;
        }
        List<MemberCouponCenterVO> memberIntegralCouponList = filterList(memberCouponList,
                res -> res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT));
        List<MemberCouponCenterVO> memberCashCouponList = filterList(memberCouponList,
                res -> !res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT));
        // 积分券
        for (MemberCouponCenterVO res : memberIntegralCouponList) {
            OrderMemberCouponVO vo = CouponConvert.INSTANCE.convert(res);
            if (orderCouponInfoHandler(res, orderBOList, vo)) {
                bucket.getAvailableIntegralList().add(vo);
            } else {
                bucket.getUnAvailableIntegralList().add(vo);
            }
        }
        // 现金券
        for (MemberCouponCenterVO res : memberCashCouponList) {
            OrderMemberCouponVO vo = CouponConvert.INSTANCE.convert(res);
            if (orderCouponInfoHandler(res, orderBOList, vo)) {
                bucket.getAvailableCashList().add(vo);
            } else {
                bucket.getUnAvailableCashList().add(vo);
            }
        }
        bucket.getAvailableIntegralList().sort(couponComparator);
        bucket.getAvailableCashList().sort(couponComparator);
        return bucket;
    }

    /**
     * 校验优惠券适用范围并计算优惠信息
     */
    private boolean orderCouponInfoHandler(MemberCouponCenterVO res, List<OrderMemberSmsCouponBO> orderBOList, OrderMemberCouponVO vo) {
        if (res.getUseStartTime().after(new Date())) {
            vo.setDisabled(CouponConstant.Disabled.UN_REACHED_TIME);
            return false;
        }
        List<OrderMemberSmsCouponBO> targetList = null;
        List<OrderMemberSmsCouponBO> tempOrderBOList = orderBOList;
        // 商家券过滤商家商品
        if (isMerchantCoupon(res.getCouponType()) && res.getMerchantId() != null) {
            tempOrderBOList = filterList(orderBOList, e -> res.getMerchantId().equals(e.getMerchantId()));
        }
        if (res.getApplyProductRangeType() != null
                && !ApplyProductRangeType.ALL.equals(res.getApplyProductRangeType())
                && CollectionUtils.isEmpty(res.getIdList())) {
            vo.setDisabled(CouponConstant.Disabled.NO_PRODUCT);
            return false;
        }
        // 适用范围匹配
        if (res.getApplyProductRangeType() == null) {
            targetList = tempOrderBOList;
        } else {
            switch (res.getApplyProductRangeType()) {
                case ALL:
                    targetList = tempOrderBOList;
                    break;
                case SPECIFY_PRODUCT:
                    targetList = filterList(tempOrderBOList,
                            e -> res.getIdList().contains(isMerchantCoupon(res.getCouponType()) ? e.getSpuId() : e.getSmsId()));
                    break;
                case EXCLUDE_PRODUCT:
                    targetList = filterList(tempOrderBOList, e ->
                            !res.getIdList().contains(isMerchantCoupon(res.getCouponType()) ? e.getSpuId() : e.getSmsId()));
                    break;
                case SPECIFY_BRAND:
                    targetList = filterList(tempOrderBOList, e -> res.getIdList().contains(e.getBrandId()));
                    break;
                case SPECIFY_CATEGORY:
                    targetList = filterList(tempOrderBOList, e -> res.getIdList().contains(e.getCategoryId()));
                    break;
                default:
            }
        }
        if (CollectionUtils.isEmpty(targetList)) {
            vo.setDisabled(CouponConstant.Disabled.NO_PRODUCT);
            return false;
        } else {
            // 按券类型计算优惠/门槛
            if (res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT)) {
                return orderIntegralCouponInfoHandler(res, targetList, vo);
            } else {
                return orderCashCouponInfoHandler(res, targetList, vo);
            }
        }
    }

    /**
     * 计算积分券的门槛、优惠金额与均摊结果
     */
    private boolean orderIntegralCouponInfoHandler(MemberCouponCenterVO res, List<OrderMemberSmsCouponBO> targetList, OrderMemberCouponVO vo) {
        int achieveIntegral = res.getAchieveAmount().intValue();
        int orderProductTotalIntegral = targetList.stream().map(e -> e.getRealIntegral() * e.getNumber()).reduce(Integer::sum).orElse(0);
        if (orderProductTotalIntegral == 0) {
            vo.setDisabled(CouponConstant.Disabled.DISCOUNT_ZERO);
            return false;
        }
        if (achieveIntegral > orderProductTotalIntegral) {
            vo.setLackAmount(BigDecimal.valueOf(achieveIntegral - orderProductTotalIntegral));
        } else {
            vo.setDiscountTotalAmount(res.getDeductionAmount());
            // 价格均摊
            Map<Long, BigDecimal> allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), true);
            vo.setAllocateResultMap(allocateResult);
        }
        return true;
    }

    /**
     * 计算现金券的门槛、优惠金额与均摊结果
     */
    private boolean orderCashCouponInfoHandler(MemberCouponCenterVO res, List<OrderMemberSmsCouponBO> targetList, OrderMemberCouponVO vo) {
        BigDecimal achieveAmount = res.getAchieveAmount();
        BigDecimal orderProductTotalAmount = targetList.stream().map(e -> e.getRealAmount().multiply(BigDecimal.valueOf(e.getNumber()))).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        if (orderProductTotalAmount.compareTo(BigDecimal.ZERO) == 0) {
            vo.setDisabled(CouponConstant.Disabled.DISCOUNT_ZERO);
            return false;
        }
        Map<Long, BigDecimal> allocateResult = null;
        switch (res.getCouponDiscountType()) {
            case DIRECT_REDUCTION:
                vo.setDiscountTotalAmount(res.getDeductionAmount().compareTo(orderProductTotalAmount) >= 0 ? orderProductTotalAmount : res.getDeductionAmount());
                // 价格均摊
                allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), false);
                break;
            case FULL_REDUCTION:
                if (achieveAmount.compareTo(orderProductTotalAmount) > 0) {
                    vo.setLackAmount(achieveAmount.subtract(orderProductTotalAmount));
                } else {
                    vo.setDiscountTotalAmount(res.getDeductionAmount().compareTo(orderProductTotalAmount) >= 0 ? orderProductTotalAmount : res.getDeductionAmount());
                    // 价格均摊
                    allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), false);
                }
                break;
            case FULL_DISCOUNT:
                if (achieveAmount.compareTo(orderProductTotalAmount) > 0) {
                    vo.setLackAmount(achieveAmount.subtract(orderProductTotalAmount));
                } else {
                    BigDecimal payAmount = orderProductTotalAmount.multiply(res.getDeductionAmount()).setScale(2, RoundingMode.HALF_UP);
                    vo.setDiscountTotalAmount(orderProductTotalAmount.subtract(payAmount));
                    // 价格均摊
                    allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), false);
                }
                break;
            default:
        }
        vo.setAllocateResultMap(allocateResult);
        return true;
    }

    /**
     * 按商品金额/积分占比分摊优惠金额
     */
    private Map<Long, BigDecimal> allocateDiscount(List<OrderMemberSmsCouponBO> targetList, BigDecimal discountAmount, boolean integralType) {
        List<MoneyUtils.MoneyBO<Long>> moneyBOList = targetList.stream().map(orderBO -> {
            MoneyUtils.MoneyBO<Long> bo = new MoneyUtils.MoneyBO<>();
            bo.setId(orderBO.getSkuId());
            bo.setPrice(integralType ? new BigDecimal(orderBO.getRealIntegral() * orderBO.getNumber()) :
                    orderBO.getRealAmount().multiply(BigDecimal.valueOf(orderBO.getNumber())));
            return bo;
        }).collect(Collectors.toList());
        return integralType ? MoneyUtils.allocateIntDiscount(moneyBOList, discountAmount.intValue()) :
                MoneyUtils.allocateBigDecimalDiscount(moneyBOList, discountAmount);
    }

    /**
     * 判断是否为商家券
     */
    private boolean isMerchantCoupon(CouponType couponType) {
        return CouponType.MERCHANT.equals(couponType);
    }
}
