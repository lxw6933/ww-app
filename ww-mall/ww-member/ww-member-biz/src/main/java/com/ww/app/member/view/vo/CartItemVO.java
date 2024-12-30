package com.ww.app.member.view.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @description: 购物车商品item详情
 * @author: ww
 * @create: 2021/7/3 下午8:22
 **/
@Data
public class CartItemVO {
    /**
     * 商品id
     */
    private Long skuId;

    /**
     * 是否选中
     */
    private Boolean check = true;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 商品属性
     */
    private List<String> skuAttr;

    /**
     * 商品价格(单价)
     */
    private BigDecimal price;

    /**
     * 商品数量
     */
    private Integer count;

    /**
     * 总价
     */
    private BigDecimal totalPrice;

    /**
     * 动态获取商品总价
     *
     * @return totalPrice
     */
    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(this.count));
    }
}
