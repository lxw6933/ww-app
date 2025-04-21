package com.ww.app.pay.dto;

import com.ww.app.pay.enums.PayChannelEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 支付查询DTO
 */
@Data
@Accessors(chain = true)
public class PaymentQueryDTO {
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
} 