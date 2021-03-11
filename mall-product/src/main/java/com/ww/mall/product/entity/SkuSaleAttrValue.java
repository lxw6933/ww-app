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
@TableName("pms_sku_sale_attr_value")
public class SkuSaleAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * id
    */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
    * sku_id
    */
    private Long skuId;

    /**
    * attr_id
    */
    private Long attrId;

    /**
    * 销售属性名
    */
    private String attrName;

    /**
    * 销售属性值
    */
    private String attrValue;

    /**
    * 顺序
    */
    private Integer attrSort;


}