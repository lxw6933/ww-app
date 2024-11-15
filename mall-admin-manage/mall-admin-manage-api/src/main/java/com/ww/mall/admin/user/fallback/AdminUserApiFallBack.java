package com.ww.mall.admin.user.fallback;

import com.ww.mall.admin.user.AdminUserApi;
import com.ww.mall.admin.user.bo.SysUserLoginBO;
import com.ww.mall.admin.user.dto.SysUserDTO;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2024-11-15- 11:36
 * @description:
 */
@Slf4j
public class AdminUserApiFallBack implements FallbackFactory<AdminUserApi> {
    @Override
    public AdminUserApi create(Throwable cause) {
        log.error("第三方服务【AdminFeignService】调用异常：{}", cause.getMessage());
        return new AdminUserApi() {
            @Override
            public Result<SysUserDTO> getAdminLoginUserInfo(SysUserLoginBO sysUserLoginBO) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

            @Override
            public Result<SysUserDTO> loadUserDetails(String username) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }
        };
    }
}
