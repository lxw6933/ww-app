package com.ww.app.cart.entity;

import lombok.Data;
import lombok.Getter;

import java.util.List;

import static com.ww.app.common.utils.CollectionUtils.getSumValue;

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
    private int countNum;

    /**
     * 购物车商品类型数量
     */
    private int countType;

    /**
     * 购物车总价格【勾选】，以分为单位
     */
    private int totalAmount;

    /**
     * 购物车扣减总金额，以分为单位
     */
    @Getter
    private int reduceAmount = 0;

    public int getCountNum() {
        return getSumValue(this.cartItems, CartItem::getCount, Integer::sum, 0);
    }

    public int getCountType() {
        return this.cartItems.size();
    }

    public int getTotalAmount() {
        return getSumValue(this.cartItems, CartItem::getTotalPrice, Integer::sum, 0);
    }

}
