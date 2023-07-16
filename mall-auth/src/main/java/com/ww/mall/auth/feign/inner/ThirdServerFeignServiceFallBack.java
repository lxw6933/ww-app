package com.ww.mall.auth.feign.inner;

import com.ww.mall.auth.feign.ThirdServerFeignService;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import org.springframework.stereotype.Component;

/**
 * @description: 服务远程接口调用触发熔断降级处理返回数据
 * 需开启 feign.sentinel.enabled=true 才生效
 * @author: ww
 * @create: 2023/7/16 17:43
 **/
@Component
public class ThirdServerFeignServiceFallBack implements ThirdServerFeignService {
    @Override
    public Result<Boolean> sendSms(String mobile, String code) {
        return new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
    }
}
