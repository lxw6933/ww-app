package com.ww.mall.product.entity.property;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2025-09-03- 09:19
 * @description: sku属性值
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "product_property_value", autoResultMap = true)
public class ProductPropertyValue extends BaseEntity {

    /**
     * 单规格，默认属性 id
     */
    public static final Long ID_DEFAULT = 0L;

    /**
     * 单规格，默认属性名字
     */
    public static final String NAME_DEFAULT = "默认";

    /**
     * 属性项的编号
     * <p>
     * 关联 {@link ProductProperty#getId()}
     */
    private Long propertyId;

    /**
     * sku属性值
     */
    private String name;

}
