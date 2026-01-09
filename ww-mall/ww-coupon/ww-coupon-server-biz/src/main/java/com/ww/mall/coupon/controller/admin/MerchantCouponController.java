package com.ww.mall.coupon.controller.admin;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.coupon.service.MerchantCouponService;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.MerchantCouponDetailVO;
import com.ww.mall.coupon.view.vo.MerchantCouponPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-07-25- 10:23
 * @description: 商家优惠券接口
 */
@RestController
@RequestMapping("/merchantCoupon")
@Tag(name = "[B端]商家优惠券 API")
public class MerchantCouponController {

    @Resource
    private MerchantCouponService merchantCouponService;

    @Operation(summary = "商家优惠券列表")
    @PostMapping("/activity/page")
    public AppPageResult<MerchantCouponPageVO> pageList(@RequestBody MerchantCouponPageBO merchantCouponPageBO) {
        return merchantCouponService.pageList(merchantCouponPageBO);
    }

    @Operation(summary = "添加商家优惠券活动")
    @PutMapping("/activity")
    public boolean add(@RequestBody @Validated MerchantCouponActivityAddBO merchantCouponActivityAddBO) {
        return merchantCouponService.add(merchantCouponActivityAddBO);
    }

    @Operation(summary = "编辑商家优惠券活动")
    @PostMapping("/activity")
    public boolean edit(@RequestBody @Validated MerchantCouponActivityEditBO merchantCouponActivityEditBO) {
        return merchantCouponService.edit(merchantCouponActivityEditBO);
    }

    @Operation(summary = "活动详情")
    @Parameters({
            @Parameter(name = "id", description = "活动id", required = true, in = ParameterIn.PATH),
    })
    @GetMapping("/activity/info/{id}")
    public MerchantCouponDetailVO info(@PathVariable("id") String id) {
        return merchantCouponService.info(id);
    }

    @Operation(summary = "上下架商家优惠券活动")
    @PostMapping("/activity/status")
    public boolean edit(@RequestBody @Validated CouponActivityStatusBO couponActivityStatusBO) {
        return merchantCouponService.status(couponActivityStatusBO);
    }

    @Operation(summary = "审核商家优惠券活动")
    @PostMapping("/activity/audit")
    public boolean edit(@RequestBody @Validated MerchantCouponActivityAuditBO merchantCouponActivityAuditBO) {
        return merchantCouponService.audit(merchantCouponActivityAuditBO);
    }

    @Operation(summary = "新增商家优惠券数量")
    @PostMapping("/addCouponCoupon")
    public boolean addCouponCoupon(@RequestBody @Validated MerchantAddCouponCodeBO addCouponCodeBO) {
        return merchantCouponService.addSmsCouponCode(addCouponCodeBO);
    }

}
