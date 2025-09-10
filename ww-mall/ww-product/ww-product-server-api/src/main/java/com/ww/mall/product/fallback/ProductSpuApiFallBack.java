package com.ww.mall.product.fallback;

import com.ww.app.common.common.Result;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.mall.product.dto.spu.ProductSpuDTO;
import com.ww.mall.product.rpc.ProductSpuApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2025-09-10 16:09
 * @description:
 */
@Slf4j
public class ProductSpuApiFallBack implements FallbackFactory<ProductSpuApi> {

    @Override
    public ProductSpuApi create(Throwable cause) {
        log.error("第三方服务【ProductFeignService】调用异常：{}", cause.getMessage());
        return new ProductSpuApi() {
            @Override
            public Result<ProductSpuDTO> getSpu(Long id) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }
        };
    }

}
