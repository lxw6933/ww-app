package com.ww.app.pay.dto;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.PayStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付回调DTO
 */
@Data
@Accessors(chain = true)
public class PaymentCallbackDTO {
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
     * 支付状态
     */
    private PayStatusEnum payStatus;
    
    /**
     * 支付金额（元）
     */
    private BigDecimal amount;
    
    /**
     * 支付完成时间
     */
    private LocalDateTime payTime;
    
    /**
     * 买家ID
     */
    private String buyerId;
    
    /**
     * 原始回调数据
     */
    private Map<String, String> rawData;
    
    /**
     * 响应数据（返回给支付平台的数据）
     */
    private String responseData;
} 