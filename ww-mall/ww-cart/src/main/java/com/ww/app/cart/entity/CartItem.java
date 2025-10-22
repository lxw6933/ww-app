package com.ww.app.cart.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 23:57
 **/
@Data
public class CartItem implements Serializable {

    /**
     * 商家id
     */
    private Long merchantId;

    /**
     * spuId
     */
    private Long spuId;

    /**
     * skuId
     */
    private Long skuId;

    /**
     * 是否选中
     */
    private boolean checked;

    /**
     * sku名称
     */
    private String title;

    /**
     * sku图片
     */
    private String image;

    /**
     * sku属性
     */
    private List<String> skuAttr;

    /**
     * sku销售价【加入时】，以分为单位
     */
    private long price;

    /**
     * 加入购书车数量
     */
    private int count;

    /**
     * 总价，以分为单位
     */
    private long totalPrice;

    /**
     * 是否失效【1：有效 0：已失效】
     */
    private boolean invalid;

    public long getTotalPrice() {
        return this.price * this.count;
    }

}
