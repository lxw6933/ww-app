package com.ww.mall.coupon.service.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.coupon.enums.ApplyProductRangeType;
import com.ww.mall.coupon.enums.CouponDiscountType;
import com.ww.mall.coupon.enums.CouponType;
import com.ww.mall.coupon.service.confirm.CouponEvaluator;
import com.ww.mall.coupon.service.strategy.PlatformFirstMerchantBestStrategy;
import com.ww.mall.coupon.view.bo.OrderMemberSmsCouponBO;
import com.ww.mall.coupon.view.vo.ConfirmOrderCouponVO;
import com.ww.mall.coupon.view.vo.MemberCouponCenterVO;
import com.ww.mall.coupon.view.vo.OrderMemberCouponVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SmsCouponServiceImplTest {

    @Test
    // 场景：平台券同时存在积分券和现金券，默认选券应优先积分券（积分优先策略）。
    void defaultPlatformPrefersIntegral() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(1L, 101L, 100, 100));
        MemberCouponCenterVO platformIntegral = coupon("P_INT", CouponType.PLATFORM,
                CouponDiscountType.INTEGRAL_DISCOUNT, BigDecimal.ZERO, BigDecimal.valueOf(10), null);
        MemberCouponCenterVO platformCash = coupon("P_CASH", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(20), null);

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(platformIntegral, platformCash),
                cloneOrders(originOrders),
                true,
                null,
                false,
                null
        );

        assertNotNull(result);
        assertEquals(1, result.getSelectedCouponList().size());
        assertEquals("P_INT", result.getSelectedCouponList().get(0).getActivityCode());
        printScenario("平台券同时存在积分券和现金券，默认选券应优先积分券（积分优先策略）", originOrders, result);
    }

    @Test
    // 场景：用户不使用平台券，仅使用商家券，系统应选择商家最优券。
    void usePlatformCouponFalseSelectsMerchantBest() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(10L, 201L, 100, 0));
        MemberCouponCenterVO merchantSmall = coupon("M_SMALL", CouponType.MERCHANT,
                CouponDiscountType.FULL_REDUCTION, BigDecimal.valueOf(200), BigDecimal.valueOf(10), 10L);
        MemberCouponCenterVO merchantBig = coupon("M_BIG", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(20), 10L);
        MemberCouponCenterVO platformCash = coupon("P_CASH", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(5), null);

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(platformCash, merchantSmall, merchantBig),
                cloneOrders(originOrders),
                false,
                null,
                true,
                null
        );

        assertNotNull(result);
        assertEquals(1, result.getSelectedCouponList().size());
        assertEquals("M_BIG", result.getSelectedCouponList().get(0).getActivityCode());
        printScenario("用户不使用平台券，仅使用商家券，系统应选择商家最优券", originOrders, result);
    }

    @Test
    // 场景：用户指定的平台券不存在或不可用，应抛出异常提示券不可用。
    void selectedPlatformCouponInvalidThrows() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(1L, 101L, 100, 0));
        MemberCouponCenterVO platformCash = coupon("P_CASH", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(10), null);

        System.out.println("=== 场景：指定平台券不存在/不可用，抛出异常 ===");
        assertThrows(ApiException.class, () -> service.getOrderMemberCouponList(
                Collections.singletonList(platformCash),
                cloneOrders(originOrders),
                true,
                "P_NOT_EXIST",
                true,
                null
        ));
    }

    @Test
    // 场景：指定的平台券存在但不满足门槛，默认不选券且不抛异常。
    void selectedPlatformCouponNotMeetThresholdDefaultNotUse() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(1L, 101L, 100, 0));
        MemberCouponCenterVO platformFull = coupon("P_FULL_200", CouponType.PLATFORM,
                CouponDiscountType.FULL_REDUCTION, BigDecimal.valueOf(200), BigDecimal.valueOf(20), null);

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Collections.singletonList(platformFull),
                cloneOrders(originOrders),
                true,
                "P_FULL_200",
                false,
                null
        );

        assertNotNull(result);
        assertTrue(result.getSelectedCouponList() == null || result.getSelectedCouponList().isEmpty());
        assertTrue(containsCoupon(safeGroup(result.getPlatformAvailable()).getCash(), "P_FULL_200"));
        printScenario("指定的平台券存在但不满足门槛，默认不选券且不抛异常", originOrders, result);
    }

    @Test
    // 场景：用户指定的商家券不存在或不可用，应抛出异常提示券不可用。
    void selectedMerchantCouponInvalidThrows() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(10L, 201L, 100, 0));
        MemberCouponCenterVO merchantCoupon = coupon("M_OK", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(10), 10L);
        Map<Long, String> selectedMerchantMap = new HashMap<>();
        selectedMerchantMap.put(10L, "M_NOT_EXIST");

        System.out.println("=== 场景：指定商家券不存在/不可用，抛出异常 ===");
        assertThrows(ApiException.class, () -> service.getOrderMemberCouponList(
                Collections.singletonList(merchantCoupon),
                cloneOrders(originOrders),
                false,
                null,
                true,
                selectedMerchantMap
        ));
    }

    @Test
    // 场景：指定的商家券存在但不满足门槛，默认不选券且不抛异常。
    void selectedMerchantCouponNotMeetThresholdDefaultNotUse() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(10L, 201L, 100, 0));
        MemberCouponCenterVO merchantFull = coupon("M_FULL_200", CouponType.MERCHANT,
                CouponDiscountType.FULL_REDUCTION, BigDecimal.valueOf(200), BigDecimal.valueOf(10), 10L);
        Map<Long, String> selectedMerchantMap = new HashMap<>();
        selectedMerchantMap.put(10L, "M_FULL_200");

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Collections.singletonList(merchantFull),
                cloneOrders(originOrders),
                false,
                null,
                true,
                selectedMerchantMap
        );

        assertNotNull(result);
        assertTrue(result.getSelectedCouponList() == null || result.getSelectedCouponList().isEmpty());
        assertTrue(containsCoupon(safeGroup(result.getMerchantAvailable()).getCash(), "M_FULL_200"));
        printScenario("指定的商家券存在但不满足门槛，默认不选券且不抛异常", originOrders, result);
    }

    @Test
    // 场景：指定商家券初始满足门槛，但选择平台券后门槛不足，商家券默认不使用。
    void selectedMerchantCouponBecomesInvalidAfterPlatformCoupon() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(10L, 201L, 100, 0));
        MemberCouponCenterVO platformCash = coupon("P_CASH_30", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(30), null);
        MemberCouponCenterVO merchantFull = coupon("M_FULL_90", CouponType.MERCHANT,
                CouponDiscountType.FULL_REDUCTION, BigDecimal.valueOf(90), BigDecimal.valueOf(10), 10L);
        Map<Long, String> selectedMerchantMap = new HashMap<>();
        selectedMerchantMap.put(10L, "M_FULL_90");

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(platformCash, merchantFull),
                cloneOrders(originOrders),
                true,
                "P_CASH_30",
                true,
                selectedMerchantMap
        );

        assertNotNull(result);
        assertTrue(containsCoupon(result.getSelectedCouponList(), "P_CASH_30"));
        assertFalse(containsCoupon(result.getSelectedCouponList(), "M_FULL_90"));
        printScenario("指定商家券初始满足门槛，但选择平台券后门槛不足，商家券默认不使用", originOrders, result);
    }

    @Test
    // 场景：仅指定部分商家券，其它商家不默认选最优券（只选择指定商家的券）。
    void partialMerchantMapSkipsOtherMerchants() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Arrays.asList(
                order(10L, 201L, 100, 0),
                order(20L, 202L, 120, 0)
        );
        MemberCouponCenterVO merchant10 = coupon("M_10", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(10), 10L);
        MemberCouponCenterVO merchant20 = coupon("M_20", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(12), 20L);
        Map<Long, String> selectedMerchantMap = new HashMap<>();
        selectedMerchantMap.put(10L, "M_10");

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(merchant10, merchant20),
                cloneOrders(originOrders),
                false,
                null,
                true,
                selectedMerchantMap
        );

        assertNotNull(result);
        assertEquals(1, result.getSelectedCouponList().size());
        assertEquals("M_10", result.getSelectedCouponList().get(0).getActivityCode());
        printScenario("仅指定部分商家券，其它商家不默认选最优券（只选择指定商家的券）", originOrders, result);
    }

    @Test
    // 场景：平台券先抵扣导致商家满减门槛不足，商家券不可选。
    void platformDiscountMakesMerchantNotSelectable() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(10L, 201L, 100, 0));
        MemberCouponCenterVO platformCash = coupon("P_CASH", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(60), null);
        MemberCouponCenterVO merchantFullReduction = coupon("M_FULL", CouponType.MERCHANT,
                CouponDiscountType.FULL_REDUCTION, BigDecimal.valueOf(80), BigDecimal.valueOf(10), 10L);

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(platformCash, merchantFullReduction),
                cloneOrders(originOrders),
                true,
                null,
                true,
                null
        );

        assertNotNull(result);
        assertEquals(1, result.getSelectedCouponList().size());
        assertEquals("P_CASH", result.getSelectedCouponList().get(0).getActivityCode());
        assertFalse(containsCoupon(result.getSelectedCouponList(), "M_FULL"));
        printScenario("平台券先抵扣导致商家满减门槛不足，商家券不可选", originOrders, result);
    }

    @Test
    // 场景：多商品+多商家+多优惠券，包含不同适用范围与门槛，验证整体分桶与默认选券。
    void mixedScenarioMultiProductMultiMerchant() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Arrays.asList(
                order(10L, 101L, 1001L, 2001L, 3001L, 4001L, 2, 120, 120),
                order(10L, 102L, 1002L, 2002L, 3002L, 4002L, 1, 80, 0),
                order(20L, 201L, 1003L, 2003L, 3003L, 4003L, 3, 60, 0)
        );
        List<MemberCouponCenterVO> coupons = new ArrayList<>();
        coupons.add(coupon("P_INT_20", CouponType.PLATFORM, CouponDiscountType.INTEGRAL_DISCOUNT,
                BigDecimal.valueOf(250), BigDecimal.valueOf(20), null));
        coupons.add(coupon("P_CASH_30", CouponType.PLATFORM, CouponDiscountType.DIRECT_REDUCTION,
                BigDecimal.ZERO, BigDecimal.valueOf(30), null));
        coupons.add(coupon("P_FULL_50", CouponType.PLATFORM, CouponDiscountType.FULL_REDUCTION,
                BigDecimal.valueOf(200), BigDecimal.valueOf(50), null));
        coupons.add(couponWithRange("P_SPEC_SMS", CouponType.PLATFORM, CouponDiscountType.DIRECT_REDUCTION,
                BigDecimal.ZERO, BigDecimal.valueOf(15), null, ApplyProductRangeType.SPECIFY_PRODUCT, Arrays.asList(1001L, 1002L)));
        coupons.add(couponWithRange("P_EXCLUDE_SMS", CouponType.PLATFORM, CouponDiscountType.DIRECT_REDUCTION,
                BigDecimal.ZERO, BigDecimal.valueOf(10), null, ApplyProductRangeType.EXCLUDE_PRODUCT, Collections.singletonList(1002L)));
        coupons.add(coupon("M10_DIR_10", CouponType.MERCHANT, CouponDiscountType.DIRECT_REDUCTION,
                BigDecimal.ZERO, BigDecimal.valueOf(10), 10L));
        coupons.add(coupon("M10_FULL_15", CouponType.MERCHANT, CouponDiscountType.FULL_REDUCTION,
                BigDecimal.valueOf(200), BigDecimal.valueOf(15), 10L));
        coupons.add(coupon("M20_FULL_12", CouponType.MERCHANT, CouponDiscountType.FULL_REDUCTION,
                BigDecimal.valueOf(150), BigDecimal.valueOf(12), 20L));
        coupons.add(couponWithRange("M20_BRAND", CouponType.MERCHANT, CouponDiscountType.DIRECT_REDUCTION,
                BigDecimal.ZERO, BigDecimal.valueOf(8), 20L, ApplyProductRangeType.SPECIFY_BRAND, Collections.singletonList(4003L)));
        coupons.add(couponWithRange("M10_CAT", CouponType.MERCHANT, CouponDiscountType.DIRECT_REDUCTION,
                BigDecimal.ZERO, BigDecimal.valueOf(6), 10L, ApplyProductRangeType.SPECIFY_CATEGORY, Collections.singletonList(3002L)));

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                coupons,
                cloneOrders(originOrders),
                true,
                null,
                true,
                null
        );

        assertNotNull(result);
        assertFalse(result.getSelectedCouponList().isEmpty());
        printScenario("多商品+多商家+多优惠券，包含不同适用范围与门槛，验证整体分桶与默认选券", originOrders, result);
    }

    @Test
    // 场景：用户明确不使用平台券和商家券，默认选中列表应为空[原价计算可用性]。
    void noPlatformNoMerchantCouponsSelected() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Arrays.asList(
                order(10L, 101L, 1001L, 2001L, 3001L, 4001L, 1, 120, 0),
                order(20L, 201L, 1002L, 2002L, 3002L, 4002L, 1, 90, 0)
        );
        MemberCouponCenterVO platformCash = coupon("P_CASH_10", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(10), null);
        MemberCouponCenterVO merchant10 = coupon("M10_DIR_5", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(5), 10L);

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(platformCash, merchant10),
                cloneOrders(originOrders),
                false,
                null,
                false,
                null
        );

        assertNotNull(result);
        assertTrue(result.getSelectedCouponList() == null || result.getSelectedCouponList().isEmpty());
        assertTrue(containsCoupon(safeGroup(result.getPlatformAvailable()).getCash(), "P_CASH_10"));
        assertTrue(containsCoupon(safeGroup(result.getMerchantAvailable()).getCash(), "M10_DIR_5"));
        printScenario("用户明确不使用平台券和商家券，默认选中列表应为空[原价计算可用性]", originOrders, result);
    }

    @Test
    // 场景：不可用展示校验（未到使用时间/适用范围为空）应进入不可用列表。
    void unavailableCouponsShouldBeInUnavailableList() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Collections.singletonList(order(10L, 301L, 100, 0));
        MemberCouponCenterVO futureCoupon = coupon("P_FUTURE", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(10), null);
        futureCoupon.setUseStartTime(new Date(System.currentTimeMillis() + 86400000L));
        MemberCouponCenterVO emptyRangeCoupon = couponWithRange("P_EMPTY_RANGE", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(5), null,
                ApplyProductRangeType.SPECIFY_PRODUCT, Collections.emptyList());

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(futureCoupon, emptyRangeCoupon),
                cloneOrders(originOrders),
                true,
                null,
                false,
                null
        );

        assertNotNull(result);
        assertTrue(containsCoupon(safeGroup(result.getUnavailable()).getCash(), "P_FUTURE"));
        assertTrue(containsCoupon(safeGroup(result.getUnavailable()).getCash(), "P_EMPTY_RANGE"));
        printScenario("不可用展示校验（未到使用时间/适用范围为空）应进入不可用列表", originOrders, result);
    }

    @Test
    // 场景：用户指定平台券 + 指定部分商家券，仅这些券进入默认选中，其它商家不自动选。
    void selectSpecificPlatformAndMerchant() {
        SmsCouponServiceImpl service = createService();
        List<OrderMemberSmsCouponBO> originOrders = Arrays.asList(
                order(10L, 101L, 1001L, 2001L, 3001L, 4001L, 1, 120, 0),
                order(20L, 201L, 1002L, 2002L, 3002L, 4002L, 1, 90, 0)
        );
        MemberCouponCenterVO platformCash = coupon("P_CASH_15", CouponType.PLATFORM,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(15), null);
        MemberCouponCenterVO platformFull = coupon("P_FULL_30", CouponType.PLATFORM,
                CouponDiscountType.FULL_REDUCTION, BigDecimal.valueOf(200), BigDecimal.valueOf(30), null);
        MemberCouponCenterVO merchant10 = coupon("M10_DIR_8", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(8), 10L);
        MemberCouponCenterVO merchant20 = coupon("M20_DIR_9", CouponType.MERCHANT,
                CouponDiscountType.DIRECT_REDUCTION, BigDecimal.ZERO, BigDecimal.valueOf(9), 20L);
        Map<Long, String> selectedMerchantMap = new HashMap<>();
        selectedMerchantMap.put(10L, "M10_DIR_8");

        ConfirmOrderCouponVO result = service.getOrderMemberCouponList(
                Arrays.asList(platformCash, platformFull, merchant10, merchant20),
                cloneOrders(originOrders),
                true,
                "P_CASH_15",
                true,
                selectedMerchantMap
        );

        assertNotNull(result);
        assertTrue(containsCoupon(result.getSelectedCouponList(), "P_CASH_15"));
        assertTrue(containsCoupon(result.getSelectedCouponList(), "M10_DIR_8"));
        assertFalse(containsCoupon(result.getSelectedCouponList(), "M20_DIR_9"));
        printScenario("用户指定平台券 + 指定部分商家券，仅这些券进入默认选中，其它商家不自动选", originOrders, result);
    }

    private SmsCouponServiceImpl createService() {
        SmsCouponServiceImpl service = new SmsCouponServiceImpl();
        setField(service, "couponEvaluator", new CouponEvaluator());
        setField(service, "defaultCouponSelectStrategy", new PlatformFirstMerchantBestStrategy());
        return service;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    private OrderMemberSmsCouponBO order(Long merchantId, Long skuId, int realAmount, int realIntegral) {
        return order(merchantId, skuId, 1000L, 2000L, 3000L, 4000L, 1, realAmount, realIntegral);
    }

    private OrderMemberSmsCouponBO order(Long merchantId,
                                         Long skuId,
                                         Long smsId,
                                         Long spuId,
                                         Long categoryId,
                                         Long brandId,
                                         int number,
                                         int realAmount,
                                         int realIntegral) {
        OrderMemberSmsCouponBO bo = new OrderMemberSmsCouponBO();
        bo.setMerchantId(merchantId);
        bo.setSmsId(smsId);
        bo.setSpuId(spuId);
        bo.setSkuId(skuId);
        bo.setCategoryId(categoryId);
        bo.setBrandId(brandId);
        bo.setNumber(number);
        bo.setRealAmount(BigDecimal.valueOf(realAmount));
        bo.setRealIntegral(realIntegral);
        return bo;
    }

    private MemberCouponCenterVO coupon(String code,
                                        CouponType couponType,
                                        CouponDiscountType discountType,
                                        BigDecimal achieveAmount,
                                        BigDecimal deductionAmount,
                                        Long merchantId) {
        MemberCouponCenterVO vo = new MemberCouponCenterVO();
        vo.setActivityCode(code);
        vo.setCouponType(couponType);
        vo.setCouponDiscountType(discountType);
        vo.setAchieveAmount(achieveAmount);
        vo.setDeductionAmount(deductionAmount);
        vo.setMerchantId(merchantId);
        vo.setApplyProductRangeType(ApplyProductRangeType.ALL);
        vo.setUseStartTime(new Date(System.currentTimeMillis() - 1000L));
        vo.setUseEndTime(new Date(System.currentTimeMillis() + 86400000L));
        return vo;
    }

    private MemberCouponCenterVO couponWithRange(String code,
                                                 CouponType couponType,
                                                 CouponDiscountType discountType,
                                                 BigDecimal achieveAmount,
                                                 BigDecimal deductionAmount,
                                                 Long merchantId,
                                                 ApplyProductRangeType rangeType,
                                                 List<Long> idList) {
        MemberCouponCenterVO vo = coupon(code, couponType, discountType, achieveAmount, deductionAmount, merchantId);
        vo.setApplyProductRangeType(rangeType);
        vo.setIdList(idList);
        return vo;
    }

    private boolean containsCoupon(List<OrderMemberCouponVO> list, String activityCode) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (OrderMemberCouponVO vo : list) {
            if (activityCode.equals(vo.getActivityCode())) {
                return true;
            }
        }
        return false;
    }

    private void printScenario(String label, List<OrderMemberSmsCouponBO> orderBOList, ConfirmOrderCouponVO result) {
        System.out.println("=== Scenario 场景: " + label + " ===");
        printOrders(orderBOList);
        printCouponGroup("平台可用积分券", safeGroup(result.getPlatformAvailable()).getIntegral());
        printCouponGroup("平台可用现金券", safeGroup(result.getPlatformAvailable()).getCash());
        printCouponGroup("商家可用积分券", safeGroup(result.getMerchantAvailable()).getIntegral());
        printCouponGroup("商家可用现金券", safeGroup(result.getMerchantAvailable()).getCash());
        printCouponGroup("不可用积分券", safeGroup(result.getUnavailable()).getIntegral());
        printCouponGroup("不可用现金券", safeGroup(result.getUnavailable()).getCash());
        printCouponGroup("选中使用优惠券", result.getSelectedCouponList());
        printFinalOrders(orderBOList, result.getSelectedCouponList());
        System.out.println();
    }

    private ConfirmOrderCouponVO.CouponGroupVO safeGroup(ConfirmOrderCouponVO.CouponGroupVO group) {
        return group == null ? new ConfirmOrderCouponVO.CouponGroupVO() : group;
    }

    private void printOrders(List<OrderMemberSmsCouponBO> orderBOList) {
        System.out.println("下单商品:");
        for (OrderMemberSmsCouponBO order : orderBOList) {
            System.out.println("  skuId=" + order.getSkuId()
                    + ", merchantId=" + order.getMerchantId()
                    + ", smsId=" + order.getSmsId()
                    + ", spuId=" + order.getSpuId()
                    + ", number=" + order.getNumber()
                    + ", realAmount=" + order.getRealAmount()
                    + ", realIntegral=" + order.getRealIntegral());
        }
    }

    private void printCouponGroup(String label, List<OrderMemberCouponVO> list) {
        System.out.println(label + ":");
        if (list == null || list.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }
        for (OrderMemberCouponVO coupon : list) {
            System.out.println("  code=" + coupon.getActivityCode()
                    + ", type=" + coupon.getCouponType()
                    + ", discountType=" + coupon.getCouponDiscountType()
                    + ", discountTotal=" + coupon.getDiscountTotalAmount()
                    + ", lack=" + coupon.getLackAmount()
                    + ", disabled=" + coupon.getDisabled()
                    + ", allocate=[" + formatAllocate(coupon.getAllocateResultMap()) + "]");
        }
    }

    private String formatAllocate(Map<Long, BigDecimal> allocateResultMap) {
        if (allocateResultMap == null || allocateResultMap.isEmpty()) {
            return "-";
        }
        List<Long> skuIds = new ArrayList<>(allocateResultMap.keySet());
        Collections.sort(skuIds);
        StringBuilder builder = new StringBuilder();
        for (Long skuId : skuIds) {
            BigDecimal value = allocateResultMap.get(skuId);
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("sku=").append(skuId).append(":").append(value);
        }
        return builder.toString();
    }

    private List<OrderMemberSmsCouponBO> cloneOrders(List<OrderMemberSmsCouponBO> origin) {
        List<OrderMemberSmsCouponBO> result = new ArrayList<>();
        for (OrderMemberSmsCouponBO order : origin) {
            OrderMemberSmsCouponBO copy = new OrderMemberSmsCouponBO();
            copy.setMerchantId(order.getMerchantId());
            copy.setSmsId(order.getSmsId());
            copy.setSpuId(order.getSpuId());
            copy.setSkuId(order.getSkuId());
            copy.setCategoryId(order.getCategoryId());
            copy.setBrandId(order.getBrandId());
            copy.setNumber(order.getNumber());
            copy.setRealAmount(order.getRealAmount());
            copy.setRealIntegral(order.getRealIntegral());
            copy.setActivityUseCoupon(order.isActivityUseCoupon());
            result.add(copy);
        }
        return result;
    }

    private void printFinalOrders(List<OrderMemberSmsCouponBO> originOrders, List<OrderMemberCouponVO> selectedCoupons) {
        List<OrderMemberSmsCouponBO> finalOrders = cloneOrders(originOrders);
        applySelectedCoupons(finalOrders, selectedCoupons);
        System.out.println("最终实付:");
        for (OrderMemberSmsCouponBO order : finalOrders) {
            System.out.println("  skuId=" + order.getSkuId()
                    + ", realAmount=" + order.getRealAmount()
                    + ", realIntegral=" + order.getRealIntegral());
        }
    }

    private void applySelectedCoupons(List<OrderMemberSmsCouponBO> orderBOList,
                                      List<OrderMemberCouponVO> selectedCoupons) {
        if (selectedCoupons == null || selectedCoupons.isEmpty()) {
            return;
        }
        List<OrderMemberCouponVO> platformCoupons = new ArrayList<>();
        List<OrderMemberCouponVO> merchantCoupons = new ArrayList<>();
        for (OrderMemberCouponVO coupon : selectedCoupons) {
            if (coupon.getCouponType() == CouponType.PLATFORM) {
                platformCoupons.add(coupon);
            } else if (coupon.getCouponType() == CouponType.MERCHANT) {
                merchantCoupons.add(coupon);
            }
        }
        for (OrderMemberCouponVO coupon : platformCoupons) {
            applyCouponToOrders(orderBOList, coupon);
        }
        for (OrderMemberCouponVO coupon : merchantCoupons) {
            applyCouponToOrders(orderBOList, coupon);
        }
    }

    private void applyCouponToOrders(List<OrderMemberSmsCouponBO> orderBOList, OrderMemberCouponVO coupon) {
        if (orderBOList == null || orderBOList.isEmpty() || coupon == null) {
            return;
        }
        Map<Long, BigDecimal> allocateMap = coupon.getAllocateResultMap();
        if (allocateMap == null || allocateMap.isEmpty()) {
            return;
        }
        boolean integralType = CouponDiscountType.INTEGRAL_DISCOUNT.equals(coupon.getCouponDiscountType());
        for (OrderMemberSmsCouponBO orderBO : orderBOList) {
            BigDecimal allocated = allocateMap.get(orderBO.getSkuId());
            if (allocated == null) {
                continue;
            }
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
                        ? adjustedTotal.divide(BigDecimal.valueOf(number), 2, BigDecimal.ROUND_HALF_UP)
                        : BigDecimal.ZERO);
            }
        }
    }
}
