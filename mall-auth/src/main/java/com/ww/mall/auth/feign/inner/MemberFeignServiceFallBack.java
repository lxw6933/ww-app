package com.ww.mall.auth.feign.inner;

import com.ww.mall.auth.feign.MemberFeignService;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.web.view.dto.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2023-07-18- 10:32
 * @description:
 */
@Slf4j
@Component
public class MemberFeignServiceFallBack implements FallbackFactory<MemberFeignService> {

    @Override
    public MemberFeignService create(Throwable cause) {
        log.error("第三方服务【MemberFeignService】调用异常", cause);
        return new MemberFeignService() {
            @Override
            public Result<MemberDTO> getMemberByMobile(String mobile) {
                return new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
            }
        };
    }
}
