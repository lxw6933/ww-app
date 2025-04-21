package com.ww.app.pay.vo;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.RefundStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 退款响应VO
 */
@Data
@Accessors(chain = true)
public class RefundResponseVO {
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 支付平台交易号
     */
    private String tradeNo;
    
    /**
     * 商户退款单号
     */
    private String outRefundNo;
    
    /**
     * 支付平台退款单号
     */
    private String refundId;
    
    /**
     * 支付渠道
     */
    private PayChannelEnum payChannel;
    
    /**
     * 退款状态
     */
    private RefundStatusEnum refundStatus;
    
    /**
     * 退款金额
     */
    private BigDecimal refundAmount;
    
    /**
     * 原始响应数据
     */
    private String rawData;
} 