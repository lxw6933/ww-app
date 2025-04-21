package com.ww.app.pay.vo;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.PayStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付查询响应VO
 */
@Data
@Accessors(chain = true)
public class PaymentQueryResponseVO {
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 支付平台交易号
     */
    private String tradeNo;
    
    /**
     * 支付渠道
     */
    private PayChannelEnum payChannel;
    
    /**
     * 支付状态
     */
    private PayStatusEnum payStatus;
    
    /**
     * 订单金额
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
     * 原始响应数据
     */
    private String rawData;
} 