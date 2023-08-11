package com.ww.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.order.enums.ActivityType;
import com.ww.mall.order.enums.RechargeAccountType;
import com.ww.mall.order.enums.RechargeType;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2023-08-11- 10:05
 * @description: 订单详情
 */
@Data
@TableName("t_order_detail")
@EqualsAndHashCode(callSuper = true)
public class OrderDetail extends BaseEntity {

    /**
     * 主订单编号
     */
    private String orderCode;

    /**
     * 子订单编号
     */
    private String subOrderCode;

    /**
     * 订单行编号
     */
    private String orderDetailCode;

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

    // ==================商品信息=====================
    /**
     * 商品编码
     */
    private Long spuCode;

    /**
     * 商品标题
     */
    private String spuTitle;

    /**
     * 商品子标题
     */
    private String spuSubTitle;

    /**
     * 规格编码
     */
    private Long skuCode;

    /**
     * 规格标题
     */
    private String skuTitle;

    /**
     * 规格图片
     */
    private String skuImg;

    /**
     * 商品品牌
     */
    private Long brandId;

    /**
     * 商品类目
     */
    private Long categoryId;

    /**
     * 商品数量
     */
    private Integer number;

    /**
     * 商品原始单价格
     */
    private BigDecimal originalPrice;

    /**
     * 是否为换购商品
     */
    private Boolean replaceProduct;
    // ==================商品信息=====================

    // ==================活动信息=====================
    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 场次活动编码【限时购多场次】
     */
    private String activitySubCode;

    /**
     * 活动类型【同一个商品只能参与一种活动】
     */
    private ActivityType activityType;

    /**
     * 优惠类型 json
     */
    private String discountType;
    // ==================活动信息=====================

    // =================虚拟商品信息===================
    /**
     * 充值类型
     */
    private RechargeType rechargeType;

    /**
     * 充值账号类型
     */
    private RechargeAccountType rechargeAccountType;

    /**
     * 充值号码
     */
    private String rechargeNo;

    /**
     * 是否充值成功
     */
    private Boolean rechargeSuccess;
    // =================虚拟商品信息===================

    // ==================金额信息=====================
    /**
     * 【商品】总运费金额
     */
    private BigDecimal freightAmount;

    /**
     * 【商品】【商家】活动总优惠金额
     */
    private BigDecimal merchantActivityDiscountAmount;

    /**
     * 【商品】【平台】活动总优惠金额
     */
    private BigDecimal platformActivityDiscountAmount;

    /**
     * 【商品】【商家】优惠券总优惠金额
     */
    private BigDecimal merchantCouponDiscountAmount;

    /**
     * 【商品】【平台】优惠券总优惠金额
     */
    private BigDecimal platformCouponDiscountAmount;

    /**
     * 【商品】订单使用总积分
     */
    private Integer orderTotalIntegral;

    /**
     * 【商品】积分优惠总金额
     */
    private Integer integralDiscountAmount;

    /**
     * 商品支付金额
     */
    private BigDecimal payPrice;
    // ==================金额信息=====================

    /**
     * 发货数量
     */
    private Integer deliverNum;

    /**
     * 售后数量
     */
    private Integer aftermarketNum;

    /**
     * 售后成功数量
     */
    private Integer aftermarketSuccessNum;

    /**
     * 权益会员code
     */
    private String equityMemberCode;

}
