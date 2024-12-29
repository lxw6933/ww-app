package com.ww.mall.member.view.vo;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * @description: 购物车详情
 * @author: ww
 * @create: 2021/7/3 下午8:22
 **/
@Data
public class CartVO {

    /**
     * 购物车内的所有商品详情记录
     */
    private List<CartItemVO> items;

    /**
     * 购物车商品总数量
     */
    private Integer countNum;

    /**
     * 商品类型数量
     */
    private Integer countType;

    /**
     * 购物车总价格（选中）
     */
    private BigDecimal totalAmount;

    /**
     * 优惠价格
     */
    private BigDecimal reduce;

    /**
     * 获取购物车商品总数量
     * 
     * @return countNum 
     */
    public Integer getCountNum() {
        this.countNum = 0;
        if (CollectionUtils.isNotEmpty(this.items)) {
            for (CartItemVO item : this.items) {
                this.countNum += item.getCount();
            }
        }
        return this.countNum;
    }

    /**
     * 获取购物车商品类型个数
     * 
     * @return countType
     */
    public Integer getCountType() {
        this.countType = 0;
        if (CollectionUtils.isNotEmpty(this.items)) {
            this.countType += 1;
        }
        return this.countType;
    }

    /**
     * 获取购物车总价格
     *
     * @return totalAmount
     */
    public BigDecimal getTotalAmount() {
        this.totalAmount = new BigDecimal(0);
        if (CollectionUtils.isNotEmpty(this.items)) {
            for (CartItemVO item : this.items) {
                this.totalAmount = this.totalAmount.add(item.getTotalPrice());
            }
        }
        this.totalAmount = this.totalAmount.subtract(getReduce());
        return this.totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

}
