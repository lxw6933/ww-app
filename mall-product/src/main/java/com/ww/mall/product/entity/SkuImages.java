package com.ww.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author ww
 * @since 2021-03-10
 */
@Data
@TableName("t_sku_img")
public class SkuImages implements Serializable {

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
     * 图片地址
     */
    private String imgUrl;

    /**
     * 排序
     */
    private Integer imgSort;

    /**
     * 默认图[0 - 不是默认图，1 - 是默认图]
     */
    private Boolean defaultImg;

}