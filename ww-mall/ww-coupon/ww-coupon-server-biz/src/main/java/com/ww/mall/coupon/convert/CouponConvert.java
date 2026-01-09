package com.ww.mall.coupon.convert;

import com.ww.mall.coupon.entity.MerchantCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.view.vo.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-19 13:53
 * @description:
 */
@Mapper
public interface CouponConvert {

    CouponConvert INSTANCE = Mappers.getMapper(CouponConvert.class);

    CouponActivityCenterVO convert(SmsCouponActivity smsCouponActivity);

    CouponActivityCenterVO convert(MerchantCouponActivity merchantCouponActivity);

    ProductCouponActivityVO convert2(SmsCouponActivity smsCouponActivity);

    SmsCouponDetailVO convert3(SmsCouponActivity smsCouponActivity);

    SmsCouponPageVO convert4(SmsCouponActivity smsCouponActivity);

    MemberCouponCenterVO convert(SmsCouponRecord smsCouponRecord);

    OrderMemberCouponVO convert(MemberCouponCenterVO memberCouponCenterVO);

    MerchantCouponPageVO convertMerchantCouponActivityToPageVO(MerchantCouponActivity merchantCouponActivity);

    MerchantCouponDetailVO convertMerchantCouponActivityToDetailVO(MerchantCouponActivity merchantCouponActivity);

}
