package com.ww.mall.coupon.controller.portal;

import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.view.bo.CouponActivityCenterBO;
import com.ww.mall.coupon.view.bo.MemberCouponCenterBO;
import com.ww.mall.coupon.view.vo.CouponActivityCenterVO;
import com.ww.mall.coupon.view.vo.MemberCouponCenterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-25- 10:23
 * @description: 优惠券接口
 */
@RestController
@RequestMapping("/coupon/portal")
@Tag(name = "优惠券服务 API")
public class PortalSmsCouponController {

    @Resource
    private SmsCouponService smsCouponService;

    @Operation(summary = "领取优惠券")
    @Parameters({
            @Parameter(name = "activityCode", description = "平台优惠券活动编码", required = true, in = ParameterIn.QUERY),
    })
    @GetMapping("/receiveCoupon")
    public boolean receiveCoupon(@RequestParam("activityCode") String activityCode) {
        return smsCouponService.receiveCoupon(activityCode);
    }

    @Operation(summary = "兑换优惠券")
    @Parameters({
            @Parameter(name = "couponCode", description = "平台优惠券券码", required = true, in = ParameterIn.QUERY),
    })
    @GetMapping("/convertCoupon")
    public boolean convertCoupon(@RequestParam("couponCode") String couponCode) {
        return smsCouponService.convertCoupon(couponCode);
    }

    @Operation(summary = "平台领券中心")
    @PostMapping("/smsCouponActivityCenter")
    public List<CouponActivityCenterVO> smsCouponActivityCenter(@RequestBody @Validated CouponActivityCenterBO couponActivityCenterBO) {
        return smsCouponService.smsCouponActivityCenter(couponActivityCenterBO);
    }

    @Operation(summary = "会员卡券中心")
    @PostMapping("/memberCouponCenter")
    public List<MemberCouponCenterVO> memberCouponCenter(@RequestBody @Validated MemberCouponCenterBO memberCouponCenterBO) {
        return smsCouponService.memberCouponCenter(memberCouponCenterBO);
    }

}
