package com.ww.mall.product.convert.brand;

import com.ww.mall.product.controller.admin.brand.req.ProductBrandBO;
import com.ww.mall.product.entity.brand.ProductBrand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-19 14:58
 * @description:
 */
@Mapper
public interface ProductBrandConvert {

    ProductBrandConvert INSTANCE = Mappers.getMapper(ProductBrandConvert.class);

    ProductBrand convert(ProductBrandBO productBrandBO);

}
