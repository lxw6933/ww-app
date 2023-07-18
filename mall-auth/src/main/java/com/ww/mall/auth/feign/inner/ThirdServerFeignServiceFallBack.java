package com.ww.mall.auth.feign.inner;

import com.ww.mall.auth.feign.ThirdServerFeignService;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @description: 服务远程接口调用触发熔断降级处理返回数据
 * 需开启 feign.sentinel.enabled=true 才生效
 * @author: ww
 * @create: 2023/7/16 17:43
 **/
@Slf4j
@Component
public class ThirdServerFeignServiceFallBack implements FallbackFactory<ThirdServerFeignService> {

    @Override
    public ThirdServerFeignService create(Throwable cause) {
        log.error("第三方服务【ThirdServerFeignService】调用异常", cause);
        return new ThirdServerFeignService() {
            @Override
            public Result<Boolean> sendSms(String mobile, String code) {
                return new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
            }
        };
    }
}
