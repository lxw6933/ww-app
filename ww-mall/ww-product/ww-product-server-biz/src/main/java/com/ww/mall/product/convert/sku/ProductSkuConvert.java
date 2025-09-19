package com.ww.mall.product.convert.sku;

import com.ww.mall.product.controller.admin.sku.req.ProductSkuBO;
import com.ww.mall.product.controller.app.spu.res.AppProductSpuDetailVO;
import com.ww.mall.product.entity.sku.ProductSku;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-19 14:32
 * @description:
 */
@Mapper
public interface ProductSkuConvert {

    ProductSkuConvert INSTANCE = Mappers.getMapper(ProductSkuConvert.class);

    ProductSku convert(ProductSkuBO productSkuBO);

    List<AppProductSpuDetailVO.Sku> convertList(List<ProductSku> skus);

}
