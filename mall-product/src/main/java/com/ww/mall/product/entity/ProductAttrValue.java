package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
* @author ww
* @since 2021-03-10
*/
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pms_product_attr_value")
public class ProductAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * id
    */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
    * 商品id
    */
    private Long spuId;

    /**
    * 属性id
    */
    private Long attrId;

    /**
    * 属性名
    */
    private String attrName;

    /**
    * 属性值
    */
    private String attrValue;

    /**
    * 顺序
    */
    private Integer attrSort;

    /**
    * 快速展示【是否展示在介绍上；0-否 1-是】
    */
    private Integer quickShow;


}