package com.ww.mall.product.convert.spu;

import com.ww.mall.product.controller.admin.spu.req.ProductSpuBO;
import com.ww.mall.product.controller.admin.spu.res.ProductSpuPageAdminVO;
import com.ww.mall.product.controller.app.spu.res.AppProductSpuDetailVO;
import com.ww.mall.product.entity.spu.ProductSpu;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-19 14:32
 * @description:
 */
@Mapper
public interface ProductSpuConvert {

    ProductSpuConvert INSTANCE = Mappers.getMapper(ProductSpuConvert.class);

    AppProductSpuDetailVO convert(ProductSpu spu);

    ProductSpuPageAdminVO convert2(ProductSpu spu);

    ProductSpu convert(ProductSpuBO productSpuBO);

}
