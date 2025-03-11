package com.ww.mall.coupon.controller;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.CouponActivityCenterVO;
import com.ww.mall.coupon.view.vo.MemberCouponCenterVO;
import com.ww.mall.coupon.view.vo.SmsCouponCodeListVO;
import com.ww.mall.coupon.view.vo.SmsCouponPageVO;
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
@RequestMapping("/coupon")
@Tag(name = "优惠券服务 API")
public class SmsCouponController {

    @Resource
    private SmsCouponService smsCouponService;

    @Operation(summary = "平台优惠券列表")
    @PostMapping("/activity")
    public AppPageResult<SmsCouponPageVO> pageList(@RequestBody SmsCouponPageBO smsCouponPageBO) {
        return smsCouponService.pageList(smsCouponPageBO);
    }

    @Operation(summary = "查看平台优惠券活动券码列表")
    @PostMapping("/activity/codes")
    public List<SmsCouponCodeListVO> codeList(@RequestBody @Validated SmsCouponCodeListBO smsCouponCodeListBO) {
        return smsCouponService.codeList(smsCouponCodeListBO);
    }

    @Operation(summary = "添加平台优惠券活动")
    @PutMapping("/activity")
    public boolean add(@RequestBody SmsCouponActivityAddBO smsCouponActivityAddBO) {
        return smsCouponService.add(smsCouponActivityAddBO);
    }

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

    @Operation(summary = "新增优惠券数量")
    @PostMapping("/addCouponCoupon")
    public boolean addCouponCoupon(@RequestBody @Validated AddCouponCodeBO addCouponCodeBO) {
        return smsCouponService.addSmsCouponCode(addCouponCodeBO);
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
