package com.ww.mall.coupon.service;

import com.ww.app.common.common.ClientUser;
import com.ww.mall.coupon.view.bo.AddCouponCodeBO;
import com.ww.mall.coupon.view.bo.SmsCouponActivityAddBO;
import com.ww.mall.coupon.view.bo.SmsCouponCodeListBO;
import com.ww.mall.coupon.view.bo.SmsCouponPageBO;
import com.ww.mall.coupon.view.vo.SmsCouponCodeListVO;
import com.ww.mall.coupon.view.vo.SmsCouponPageVO;
import com.ww.app.common.common.AppPageResult;

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
     * 添加优惠券数量
     *
     * @param addCouponCodeBO bo
     * @return boolean
     */
    boolean addSmsCouponCode(AddCouponCodeBO addCouponCodeBO);

    /**
     * 更新用户优惠券状态信息
     *
     * @param clientUser 用户
     */
    void updateMemberCouponStatus(ClientUser clientUser);

}
