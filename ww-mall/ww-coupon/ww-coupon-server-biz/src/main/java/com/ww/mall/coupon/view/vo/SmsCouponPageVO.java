package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.IssueType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
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
public class SmsCouponPageVO extends BaseCouponInfoVO {

    /**
     * 发放类型
     */
    private IssueType issueType;

    /**
     * 初始化优惠券数量
     */
    private int number;

    /**
     * 优惠券剩余可用数量
     */
    private int availableNumber;

    /**
     * 已领取数量
     */
    private int receiveNumber;

    /**
     * 已使用数量
     */
    private int useNumber;

    /**
     * 开始领取时间
     */
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    private Date receiveEndTime;

    /**
     * 上下架状态
     */
    private Boolean status;

    /**
     * 创建时间
     */
    private String createTime;

}
