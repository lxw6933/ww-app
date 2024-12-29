package com.ww.mall.pay.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2024-06-05- 10:46
 * @description:
 */
@Data
public class PayOrderJrn {

    private Long id;

    /**
     * 支付状态
     * 0: 待支付
     * 1：支付中
     * 2：支付成功
     * 3：支付失败
     * 4：已过期
     */
    private Integer status;

    /**
     * 支付订单编号【商户】
     */
    private String payOrderCode;

    /**
     * 支付订单编号【三方：微信、支付宝、银联等】
     */
    private String thirdPayOrderCode;

    /**
     * 支付流水号【三方：微信、支付宝、银联等】
     */
    private String thirdPayOrderJrn;

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付备注
     */
    private String remark;

    /**
     * 支付截止时间
     */
    private Date payCloseTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
