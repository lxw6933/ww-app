package com.ww.app.pay.dto;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.PayTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付请求DTO
 */
@Data
@Accessors(chain = true)
public class PaymentRequestDTO {
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 订单标题
     */
    private String subject;
    
    /**
     * 订单描述
     */
    private String body;
    
    /**
     * 支付金额（元）
     */
    private BigDecimal amount;
    
    /**
     * 支付渠道
     */
    private PayChannelEnum payChannel;
    
    /**
     * 支付方式
     */
    private PayTypeEnum payType;
    
    /**
     * 用户IP
     */
    private String clientIp;
    
    /**
     * 通知地址
     */
    private String notifyUrl;
    
    /**
     * 前端回调地址
     */
    private String returnUrl;
    
    /**
     * 过期时间(分钟)
     */
    private Integer expireTime;
    
    /**
     * 产品编号，扫码支付使用
     */
    private String productId;
    
    /**
     * 用户标识，公众号支付、小程序支付使用
     */
    private String openId;
    
    /**
     * 自定义参数
     */
    private Map<String, String> extraParams;
} 