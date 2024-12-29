package com.ww.mall.cart.entity;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

import static com.ww.mall.common.utils.CollectionUtils.getSumValue;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 00:01
 **/
@Data
public class Cart {
    /**
     * 购物车明细
     */
    private List<CartItem> cartItems;

    /**
     * 购物车商品总数量
     */
    private Integer countNum;

    /**
     * 购物车商品类型数量
     */
    private Integer countType;

    /**
     * 购物车总价格【勾选】
     */
    private BigDecimal totalAmount;

    /**
     * 购物车扣减总金额
     */
    @Getter
    private BigDecimal reduceAmount = BigDecimal.ZERO;

    public Integer getCountNum() {
        return getSumValue(this.cartItems, CartItem::getCount, Integer::sum, 0);
    }

    public Integer getCountType() {
        return this.cartItems.size();
    }

    public BigDecimal getTotalAmount() {
        return getSumValue(this.cartItems, CartItem::getTotalPrice, BigDecimal::add, BigDecimal.ZERO);
    }

}
