package com.ww.app.cart.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
    private List<CartItem> cartItems = new ArrayList<>();

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
    private long totalAmount;

    public void recalcTotals() {
        this.countNum = cartItems.stream().mapToInt(CartItem::getCount).sum();
        this.countType = cartItems.size();
        this.totalAmount = cartItems.stream()
                .filter(CartItem::isChecked)
                .mapToLong(CartItem::getTotalPrice)
                .sum();
    }

}
