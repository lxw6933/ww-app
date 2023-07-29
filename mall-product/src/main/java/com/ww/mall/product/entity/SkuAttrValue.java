package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @since 2021-03-10
 */
@Data
@TableName("t_sku_attr_value")
public class SkuAttrValue implements Serializable {

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
     * spu_id
     */
    private Long spuId;

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
    private Integer sort;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 是否删除
     */
    private Boolean deleted;

}