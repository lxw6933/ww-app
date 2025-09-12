package com.ww.mall.product.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.mall.product.cache.ProductSpuCache;
import com.ww.mall.product.entity.brand.ProductBrand;
import com.ww.mall.product.entity.sku.ProductSku;
import com.ww.mall.product.entity.spu.ProductSpu;
import com.ww.mall.product.service.sku.ProductSkuService;
import com.ww.mall.product.service.spu.ProductSpuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ww.mall.product.enums.ErrorCodeConstants.SPU_NOT_EXISTS;

/**
 * @author ww
 * @create 2025-09-12 17:06
 * @description:
 */
@Slf4j
@Configuration
public class CaffeineCacheConfiguration {

    @Resource
    private ProductSpuService productSpuService;

    @Resource
    private ProductSkuService productSkuService;

    @Bean
    public Cache<Long, ProductBrand> productBrandCache() {
        return CaffeineUtil.createCache(100, 500, 1, TimeUnit.DAYS);
    }

    @Bean
    public LoadingCache<Long, ProductSpuCache> spuCache() {
        return CaffeineUtil.createAutoRefreshCache(
                500,
                2000,
                1,
                TimeUnit.HOURS,
                30,
                TimeUnit.MINUTES,
                spuId -> {
                    log.info("spu[{}]本地缓存未命中，进入数据库查询", spuId);
                    ProductSpuCache cache = new ProductSpuCache();
                    // 获得商品 SPU
                    ProductSpu spu = productSpuService.getById(spuId);
                    if (spu == null) {
                        log.error("spu[{}]数据库查询为null", spuId);
                        throw new ApiException(SPU_NOT_EXISTS);
                    }
                    // 获得商品 SKU
                    List<ProductSku> skus = productSkuService.getSkuListBySpuId(spu.getId());
                    cache.setSpu(spu);
                    cache.setSkus(skus);
                    return cache;
                }
        );
    }

}
