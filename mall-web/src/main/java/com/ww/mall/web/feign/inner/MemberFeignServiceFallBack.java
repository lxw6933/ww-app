package com.ww.mall.web.feign.inner;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.web.feign.MemberFeignService;
import com.ww.mall.web.view.bo.AddMemberIntegralBO;
import com.ww.mall.web.view.dto.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2023-07-18- 10:32
 * @description:
 */
@Slf4j
public class MemberFeignServiceFallBack implements FallbackFactory<MemberFeignService> {

    @Override
    public MemberFeignService create(Throwable cause) {
        log.error("第三方服务【MemberFeignService】调用异常：{}", cause.getMessage());
        return new MemberFeignService() {
            @Override
            public Result<MemberDTO> getMemberByMobile(String mobile) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

            @Override
            public Result<Boolean> addMemberIntegral(AddMemberIntegralBO addMemberIntegralBO) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }
        };
    }
}
