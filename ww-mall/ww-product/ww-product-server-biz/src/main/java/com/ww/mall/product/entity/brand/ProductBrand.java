package com.ww.mall.product.entity.brand;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2025-09-03- 09:51
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("product_brand")
public class ProductBrand extends BaseEntity {

    /**
     * 品牌名称
     */
    private String name;

    /**
     * 品牌图片
     */
    private String img;

    /**
     * 品牌排序
     */
    private Integer sort;

    /**
     * 品牌描述
     */
    private String description;

    /**
     * 状态 [0: 禁用  1：启用]
     */
    private Boolean status;

}
