package com.ww.mall.product.entity.property;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2025-09-03- 09:19
 * @description: sku属性名
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "product_property", autoResultMap = true)
public class ProductProperty extends BaseEntity {

    /**
     * 单规格，默认属性 id
     */
    public static final Long ID_DEFAULT = 0L;

    /**
     * 单规格，默认属性名字
     */
    public static final String NAME_DEFAULT = "默认";

    /**
     * 属性名称
     */
    private String name;

}
