package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.mall.web.cmmon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author ww
 * @since 2021-03-10
 */
@Data
@TableName("t_sku")
@EqualsAndHashCode(callSuper = true)
public class Sku extends BaseEntity {

    /**
     * skuId
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
    private Boolean status;

    /**
     * 是否有效
     */
    private Boolean valid;

}