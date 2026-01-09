package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.enums.IssueType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 平台优惠券列表信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台优惠券列表信息")
public class SmsCouponPageVO extends BaseCouponInfoVO {

    /**
     * 发放类型
     */
    @Schema(description = "发放类型", example = "RECEIVE", allowableValues = {"RECEIVE", "ADMIN_ISSUE", "API_ISSUE", "EXPORT_ISSUE"})
    private IssueType issueType;

    /**
     * 初始化优惠券数量
     */
    @Schema(description = "初始化优惠券数量", example = "1000")
    private int number;

    /**
     * 优惠券剩余可用数量
     */
    @Schema(description = "优惠券剩余可用数量", example = "850")
    private int availableNumber;

    /**
     * 已领取数量
     */
    @Schema(description = "已领取数量", example = "150")
    private int receiveNumber;

    /**
     * 已使用数量
     */
    @Schema(description = "已使用数量", example = "50")
    private int useNumber;

    /**
     * 开始领取时间
     */
    @Schema(description = "开始领取时间", example = "2025-01-01 00:00:00")
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    @Schema(description = "结束领取时间", example = "2025-12-31 23:59:59")
    private Date receiveEndTime;

    /**
     * 上下架状态
     */
    @Schema(description = "上下架状态", example = "true")
    private Boolean status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-01-01 00:00:00")
    private Date createTime;
    
}
