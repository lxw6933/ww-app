package com.ww.app.pay.dto;

import com.ww.app.pay.enums.PayChannelEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 退款查询DTO
 */
@Data
@Accessors(chain = true)
public class RefundQueryDTO {
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
} 