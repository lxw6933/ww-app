package com.ww.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.mybatis.common.BaseEntity;
import com.ww.mall.order.enums.OrderSubStatus;
import com.ww.mall.order.enums.ProductType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2023-08-11- 10:05
 * @description: 子订单【根据商家拆单、再根据实物虚拟商品拆单】
 */
@Data
@TableName("t_order_sub")
@EqualsAndHashCode(callSuper = true)
public class OrderSub extends BaseEntity {

    /**
     * 主订单编号
     */
    private String orderCode;

    /**
     * 子订单编号
     */
    private String subOrderCode;

    /**
     * 商家id
     */
    private Long merchantId;

    /**
     * 渠道ID
     */
    private Long channelId;

    /**
     * 订单用户
     */
    private String userId;

    /**
     * 商品类型
     */
    private ProductType productType;

    /**
     * 子订单状态
     */
    private OrderSubStatus orderSubStatus;

    /**
     * 发货时间
     */
    private Date deliveryTime;

    /**
     * 订单完成时间
     */
    private Date completeTime;

    /**
     * 【子订单】总运费金额
     */
    private BigDecimal freightAmount;

    /**
     * 【子订单】【商家】活动总优惠金额
     */
    private BigDecimal merchantActivityDiscountAmount;

    /**
     * 【子订单】【平台】活动总优惠金额
     */
    private BigDecimal platformActivityDiscountAmount;

    /**
     * 【子订单】【商家】优惠券总优惠金额
     */
    private BigDecimal merchantCouponDiscountAmount;

    /**
     * 【子订单】【平台】优惠券总优惠金额
     */
    private BigDecimal platformCouponDiscountAmount;

    /**
     * 【子订单】订单使用总积分
     */
    private Integer orderTotalIntegral;

    /**
     * 【子订单】积分优惠总金额
     */
    private Integer integralDiscountAmount;

    /**
     * 【子订单】实付金额
     */
    private BigDecimal payAmount;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人手机号
     */
    private String receiverPhone;

    /**
     * 收货地邮编
     */
    private String receiverPostCode;

    /**
     * 收货省份
     */
    private String receiverProvince;

    /**
     * 收货城市
     */
    private String receiverCity;

    /**
     * 收货区县
     */
    private String receiverArea;

    /**
     * 收货街道
     */
    private String receiverStreet;

    /**
     * 收货详细地址
     */
    private String receiverDetail;

    /**
     * 订单备注
     */
    private String remark;

}
