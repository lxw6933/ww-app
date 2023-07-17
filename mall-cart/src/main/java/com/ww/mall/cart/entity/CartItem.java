package com.ww.mall.cart.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 23:57
 **/
@Data
public class CartItem implements Serializable {

    private Long skuId;

    private Boolean checked;

    private String title;

    private String image;

    private List<String> skuAttr;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

    public BigDecimal getTotalPrice() {
        return this.price.multiply(BigDecimal.valueOf(this.count));
    }

}
