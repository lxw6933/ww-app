package com.ww.app.pay.vo;

import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.PayTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付响应VO
 */
@Data
@Accessors(chain = true)
public class PaymentResponseVO {
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 支付渠道
     */
    private PayChannelEnum payChannel;
    
    /**
     * 支付方式
     */
    private PayTypeEnum payType;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 支付跳转链接，H5支付、PC支付使用
     */
    private String payUrl;
    
    /**
     * 支付二维码链接，扫码支付使用
     */
    private String qrCodeUrl;
    
    /**
     * 支付表单，部分渠道使用表单提交
     */
    private String formHtml;
    
    /**
     * 支付参数，APP支付、小程序支付、JSAPI支付使用
     */
    private Map<String, String> payParams;
    
    /**
     * H5支付参数，iOS需使用UrlEncode
     */
    private String mwebUrl;
} 