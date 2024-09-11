package com.ww.mall.web.feign.inner;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.web.feign.AdminFeignService;
import com.ww.mall.web.view.bo.SysUserLoginBO;
import com.ww.mall.web.view.dto.SysUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2023-07-18- 10:32
 * @description:
 */
@Slf4j
public class AdminFeignServiceFallBack implements FallbackFactory<AdminFeignService> {

    @Override
    public AdminFeignService create(Throwable cause) {
        log.error("第三方服务【AdminFeignService】调用异常：{}", cause.getMessage());
        return new AdminFeignService() {
            @Override
            public Result<SysUserDTO> login(SysUserLoginBO sysUserLoginBO) {
                return new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
            }
        };
    }
}
