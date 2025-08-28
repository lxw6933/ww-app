package com.ww.mall.coupon.controller.admin;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.*;
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
@Tag(name = "[B端]优惠券 API")
public class SmsCouponController {

    @Resource
    private SmsCouponService smsCouponService;

    @Operation(summary = "平台优惠券列表")
    @PostMapping("/activity/page")
    public AppPageResult<SmsCouponPageVO> pageList(@RequestBody SmsCouponPageBO smsCouponPageBO) {
        return smsCouponService.pageList(smsCouponPageBO);
    }

    @Operation(summary = "查看平台优惠券活动券码列表")
    @PostMapping("/activity/codes")
    public List<SmsCouponCodeListVO> codeList(@RequestBody @Validated SmsCouponCodeListBO smsCouponCodeListBO) {
        return smsCouponService.codeList(smsCouponCodeListBO);
    }

    @Operation(summary = "获取活动所有批次号")
    @PostMapping("/queryActivityBatchNoList")
    public List<String> queryActivityBatchNoList(@RequestBody @Validated SmsCouponActivityBatchNoBO batchNoBO) {
        return smsCouponService.queryActivityBatchNoList(batchNoBO);
    }

    @Operation(summary = "导出平台优惠券活动券码列表")
    @PostMapping("/exportCouponCode")
    public String exportCouponCode(@RequestBody @Validated SmsCouponCodeListBO smsCouponCodeListBO) {
        return smsCouponService.exportCouponCode(smsCouponCodeListBO);
    }

    @Operation(summary = "添加平台优惠券活动")
    @PutMapping("/activity")
    public boolean add(@RequestBody @Validated SmsCouponActivityAddBO smsCouponActivityAddBO) {
        return smsCouponService.add(smsCouponActivityAddBO);
    }

    @Operation(summary = "编辑平台优惠券活动")
    @PostMapping("/activity")
    public boolean edit(@RequestBody @Validated SmsCouponActivityEditBO smsCouponActivityEditBO) {
        return smsCouponService.edit(smsCouponActivityEditBO);
    }

    @Operation(summary = "活动详情")
    @Parameters({
            @Parameter(name = "id", description = "活动id", required = true, in = ParameterIn.PATH),
    })
    @GetMapping("/activity/info/{id}")
    public SmsCouponDetailVO info(@PathVariable("id") String id) {
        return smsCouponService.info(id);
    }

    @Operation(summary = "上下架优惠券活动")
    @PostMapping("/activity/status")
    public boolean edit(@RequestBody @Validated SmsCouponActivityStatusBO smsCouponActivityStatusBO) {
        return smsCouponService.status(smsCouponActivityStatusBO);
    }

    @Operation(summary = "新增优惠券数量")
    @PostMapping("/addCouponCoupon")
    public boolean addCouponCoupon(@RequestBody @Validated AddCouponCodeBO addCouponCodeBO) {
        return smsCouponService.addSmsCouponCode(addCouponCodeBO);
    }

}
