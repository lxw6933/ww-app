package com.ww.app.pay.dto;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.RefundStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 退款回调DTO
 */
@Data
@Accessors(chain = true)
public class RefundCallbackDTO {
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
     * 支付平台退款单号
     */
    private String refundId;
    
    /**
     * 退款状态
     */
    private RefundStatusEnum refundStatus;
    
    /**
     * 退款金额（元）
     */
    private BigDecimal refundAmount;
    
    /**
     * 退款完成时间
     */
    private LocalDateTime refundTime;
    
    /**
     * 原始回调数据
     */
    private Map<String, String> rawData;
    
    /**
     * 响应数据（返回给支付平台的数据）
     */
    private String responseData;
} 