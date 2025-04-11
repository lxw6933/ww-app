package com.ww.app.flink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件模型
 * 用于模拟电商平台的订单数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单状态
     * CREATE - 创建
     * PAY - 支付
     * DELIVER - 发货
     * RECEIVE - 收货
     * COMPLETE - 完成
     * CANCEL - 取消
     */
    private String status;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 支付方式：
     * ALIPAY - 支付宝
     * WECHAT - 微信
     * CREDIT_CARD - 信用卡
     * OTHER - 其他
     */
    private String paymentType;
    
    /**
     * 订单来源渠道
     * APP - 应用
     * WEB - 网站
     * MINI_PROGRAM - 小程序
     * OTHER - 其他
     */
    private String channel;
    
    /**
     * 商品分类ID
     */
    private Integer categoryId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品数量
     */
    private Integer quantity;
    
    /**
     * 事件时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 额外信息，JSON格式
     */
    private String extraInfo;
    
    /**
     * 地区编码
     */
    private String regionCode;
} 