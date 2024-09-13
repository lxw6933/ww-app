package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.mybatisplus.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_sku")
@EqualsAndHashCode(callSuper = true)
public class Sku extends BaseEntity {

    /**
     * spuId
     */
    private Long spuId;

    /**
     * sku编码
     */
    private String skuCode;

    /**
     * sku名称
     */
    private String skuName;

    /**
     * sku图片
     */
    private String skuImg;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 是否启用
     */
    private Boolean state;

    /**
     * 是否有效
     */
    private Boolean valid;

}