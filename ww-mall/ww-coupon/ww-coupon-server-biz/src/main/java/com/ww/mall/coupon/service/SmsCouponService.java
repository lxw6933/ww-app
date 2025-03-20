package com.ww.mall.coupon.service;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.CouponActivityCenterVO;
import com.ww.mall.coupon.view.vo.MemberCouponCenterVO;
import com.ww.mall.coupon.view.vo.SmsCouponCodeListVO;
import com.ww.mall.coupon.view.vo.SmsCouponPageVO;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-25- 10:19
 * @description:
 */
public interface SmsCouponService {

    /**
     * 优惠券分页列表
     *
     * @param smsCouponPageBO pageBO
     * @return MallPageResult
     */
    AppPageResult<SmsCouponPageVO> pageList(SmsCouponPageBO smsCouponPageBO);

    /**
     * 优惠券券码列表
     *
     * @param smsCouponCodeListBO 查询条件
     * @return 券码列表
     */
    List<SmsCouponCodeListVO> codeList(SmsCouponCodeListBO smsCouponCodeListBO);

    /**
     * 新增优惠券活动
     *
     * @param smsCouponActivityAddBO smsCouponActivityBO
     * @return boolean
     */
    boolean add(SmsCouponActivityAddBO smsCouponActivityAddBO);

    /**
     * 编辑优惠券活动
     *
     * @param smsCouponActivityEditBO smsCouponActivityEditBO
     * @return boolean
     */
    boolean edit(SmsCouponActivityEditBO smsCouponActivityEditBO);

    /**
     * 上下架活动
     *
     * @param smsCouponActivityStatusBO bo
     * @return boolean
     */
    boolean status(SmsCouponActivityStatusBO smsCouponActivityStatusBO);

    /**
     * 用户领取优惠券
     *
     * @param activityCode 优惠券编码
     * @return boolean
     */
    boolean receiveCoupon(String activityCode);

    /**
     * 兑换优惠券
     *
     * @param couponCode 优惠券券码
     * @return boolean
     */
    boolean convertCoupon(String couponCode);

    /**
     * 获取活动现有所有批次号
     *
     * @param batchNoBO 批次号条件
     * @return List<String>
     */
    List<String> queryActivityBatchNoList(SmsCouponActivityBatchNoBO batchNoBO);

    /**
     * 添加优惠券数量
     *
     * @param addCouponCodeBO bo
     * @return boolean
     */
    boolean addSmsCouponCode(AddCouponCodeBO addCouponCodeBO);

    /**
     * 平台领券中心
     *
     * @param bo 查询条件
     * @return List<CouponActivityCenterVO>
     */
    List<CouponActivityCenterVO> smsCouponActivityCenter(CouponActivityCenterBO bo);

    /**
     * 会员优惠券卡包
     *
     * @param bo 查询条件
     * @return List<MemberCouponCenterVO>
     */
    List<MemberCouponCenterVO> memberCouponCenter(MemberCouponCenterBO bo);

}
