package com.ww.mall.product.cache;

import com.ww.mall.product.entity.sku.ProductSku;
import com.ww.mall.product.entity.spu.ProductSpu;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-12 17:12
 * @description:
 */
@Data
public class ProductSpuCache {

    private ProductSpu spu;

    private List<ProductSku> skus;

}
