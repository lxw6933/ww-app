package com.ww.app.pay.vo;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.RefundStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款查询响应VO
 */
@Data
@Accessors(chain = true)
public class RefundQueryResponseVO {
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
     * 退款时间
     */
    private LocalDateTime refundTime;
    
    /**
     * 原始响应数据
     */
    private String rawData;
} 