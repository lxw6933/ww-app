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
@TableName("pms_sku_images")
public class SkuImages implements Serializable {

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
    private Integer defaultImg;


}