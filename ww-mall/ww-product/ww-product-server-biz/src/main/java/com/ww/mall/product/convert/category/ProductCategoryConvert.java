package com.ww.mall.product.convert.category;

import com.ww.mall.product.controller.admin.category.req.ProductCategoryBO;
import com.ww.mall.product.controller.admin.category.res.ProductCategoryVO;
import com.ww.mall.product.entity.category.ProductCategory;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-19 14:58
 * @description:
 */
@Mapper
public interface ProductCategoryConvert {

    ProductCategoryConvert INSTANCE = Mappers.getMapper(ProductCategoryConvert.class);

    ProductCategory convert(ProductCategoryBO productCategoryBO);

    List<ProductCategoryVO> convertList(List<ProductCategory> categories);

}
