package com.ww.mall.coupon.service;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.CouponActivityCenterVO;
import com.ww.mall.coupon.view.vo.MerchantCouponDetailVO;
import com.ww.mall.coupon.view.vo.MerchantCouponPageVO;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-25- 10:19
 * @description:
 */
public interface MerchantCouponService {

    /**
     * 商家优惠券分页列表
     *
     * @param merchantCouponPageBO pageBO
     * @return MallPageResult
     */
    AppPageResult<MerchantCouponPageVO> pageList(MerchantCouponPageBO merchantCouponPageBO);

    /**
     * 新增商家优惠券活动
     *
     * @param merchantCouponActivityAddBO merchantCouponActivityAddBO
     * @return boolean
     */
    boolean add(MerchantCouponActivityAddBO merchantCouponActivityAddBO);

    /**
     * 编辑商家优惠券活动
     *
     * @param merchantCouponActivityEditBO merchantCouponActivityEditBO
     * @return boolean
     */
    boolean edit(MerchantCouponActivityEditBO merchantCouponActivityEditBO);

    /**
     * 商家优惠券活动详情
     *
     * @param id 活动id
     * @return MerchantCouponDetailVO
     */
    MerchantCouponDetailVO info(String id);

    /**
     * 上下架活动
     *
     * @param couponActivityStatusBO bo
     * @return boolean
     */
    boolean status(CouponActivityStatusBO couponActivityStatusBO);

    /**
     * 商家优惠券活动审核
     *
     * @param merchantCouponActivityAuditBO bo
     * @return boolean
     */
    boolean audit(MerchantCouponActivityAuditBO merchantCouponActivityAuditBO);

    /**
     * 添加商家优惠券数量
     *
     * @param addCouponCodeBO bo
     * @return boolean
     */
    boolean addSmsCouponCode(MerchantAddCouponCodeBO addCouponCodeBO);

    /**
     * 店铺领券中心
     *
     * @param bo 查询条件
     * @return List<CouponActivityCenterVO>
     */
    List<CouponActivityCenterVO> merchantCouponActivityCenter(MerchantCouponActivityCenterBO bo);
}
