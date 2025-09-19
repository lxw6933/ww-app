package com.ww.mall.product.convert.property;

import com.ww.mall.product.controller.admin.property.req.ProductPropertyBO;
import com.ww.mall.product.controller.admin.property.req.ProductPropertyValueBO;
import com.ww.mall.product.entity.property.ProductProperty;
import com.ww.mall.product.entity.property.ProductPropertyValue;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-19 14:58
 * @description:
 */
@Mapper
public interface ProductPropertyConvert {

    ProductPropertyConvert INSTANCE = Mappers.getMapper(ProductPropertyConvert.class);

    ProductProperty convert(ProductPropertyBO productPropertyBO);

    ProductPropertyValue convert(ProductPropertyValueBO productPropertyValueBO);

}
