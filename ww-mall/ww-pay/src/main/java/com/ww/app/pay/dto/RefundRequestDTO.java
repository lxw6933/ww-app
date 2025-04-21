package com.ww.app.pay.dto;

import com.ww.app.pay.enums.PayChannelEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
@Accessors(chain = true)
public class RefundRequestDTO {
    /**
     * 支付渠道
     */
    private PayChannelEnum payChannel;
    
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
     * 退款金额（元）
     */
    private BigDecimal refundAmount;
    
    /**
     * 订单总金额（元）
     */
    private BigDecimal totalAmount;
    
    /**
     * 退款原因
     */
    private String refundReason;
    
    /**
     * 退款通知地址
     */
    private String notifyUrl;
} 