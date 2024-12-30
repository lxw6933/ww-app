package com.ww.app.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import com.ww.app.order.enums.OrderSourceType;
import com.ww.app.order.enums.OrderStatus;
import com.ww.app.order.enums.OrderType;
import com.ww.app.order.enums.PayType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2023-08-11- 09:29
 * @description: 主订单
 */
@Data
@TableName("t_order")
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {

    /**
     * 渠道ID
     */
    private Long channelId;

    /**
     * 订单编号
     */
    private String orderCode;

    /**
     * 订单用户
     */
    private String userId;

    /**
     * 订单状态
     */
    private OrderStatus orderStatus;

    /**
     * 订单类型
     */
    private OrderType orderType;

    /**
     * 支付类型
     */
    private PayType payType;

    /**
     * 分期数
     */
    private Integer stageNum;

    /**
     * 分期费率
     */
    private BigDecimal stageRate;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 总运费金额
     */
    private BigDecimal freightAmount;

    /**
     * 【商家】活动总优惠金额
     */
    private BigDecimal merchantActivityDiscountAmount;

    /**
     * 【平台】活动总优惠金额
     */
    private BigDecimal platformActivityDiscountAmount;

    /**
     * 【商家】优惠券总优惠金额
     */
    private BigDecimal merchantCouponDiscountAmount;

    /**
     * 【平台】优惠券总优惠金额
     */
    private BigDecimal platformCouponDiscountAmount;

    /**
     * 订单使用总积分
     */
    private Integer orderTotalIntegral;

    /**
     * 积分优惠总金额
     */
    private Integer integralDiscountAmount;

    /**
     * 订单实付金额【尾款实付金额】
     */
    private BigDecimal payAmount;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 支付关闭时间
     */
    private Date payCloseTime;

    /**
     * 是否申请售后
     */
    private Boolean applyAfterSale;

    /**
     * 售后期限时间
     */
    private Date afterCloseTime;

    /**
     * 订单定金（预售）无法使用优惠
     */
    private BigDecimal orderDeposit;

    /**
     * 定金总膨胀金额（预售）
     */
    private BigDecimal swellAmount;

    /**
     * 来源类型
     */
    private OrderSourceType sourceType;

    /**
     * 来源id
     */
    private String sourceId;

}
