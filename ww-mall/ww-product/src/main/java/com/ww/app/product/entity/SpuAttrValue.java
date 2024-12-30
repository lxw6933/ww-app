package com.ww.app.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

import lombok.Data;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@TableName("t_spu_attr_value")
public class SpuAttrValue implements Serializable {

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
