package com.ww.mall.product.rpc;

import com.ww.app.common.common.Result;
import com.ww.mall.product.convert.spu.ProductSpuConvert;
import com.ww.mall.product.dto.spu.ProductSpuDTO;
import com.ww.mall.product.entity.spu.ProductSpu;
import com.ww.mall.product.service.spu.ProductSpuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-09-10 16:21
 * @description:
 */
@RestController
@RequestMapping(ProductSpuApi.PREFIX)
public class ProductSpuApiRpc implements ProductSpuApi {

    @Resource
    private ProductSpuService productSpuService;

    @Override
    @RequestMapping("/get")
    public Result<ProductSpuDTO> getSpu(Long id) {
        ProductSpu spu = productSpuService.get(id);
        return Result.success(ProductSpuConvert.INSTANCE.convert3(spu));
    }

}
