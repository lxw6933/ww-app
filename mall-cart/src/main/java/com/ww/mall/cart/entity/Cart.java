package com.ww.mall.cart.entity;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
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
    private BigDecimal reduceAmount = BigDecimal.ZERO;

    public Integer getCountNum() {
        if (CollectionUtils.isNotEmpty(this.cartItems)) {
            return this.cartItems.stream()
                    .map(CartItem::getCount)
                    .reduce(Integer::sum)
                    .orElse(0);
        } else {
            return 0;
        }
    }

    public Integer getCountType() {
        return this.cartItems.size();
    }

    public BigDecimal getTotalAmount() {
        if (CollectionUtils.isNotEmpty(this.cartItems)) {
            BigDecimal cartSkuTotalAmount = this.cartItems.stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);
            cartSkuTotalAmount = cartSkuTotalAmount.subtract(getReduceAmount());
            cartSkuTotalAmount = cartSkuTotalAmount.compareTo(BigDecimal.ZERO) > 0 ? cartSkuTotalAmount : BigDecimal.ZERO;
            return cartSkuTotalAmount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getReduceAmount() {
        return reduceAmount;
    }
}
