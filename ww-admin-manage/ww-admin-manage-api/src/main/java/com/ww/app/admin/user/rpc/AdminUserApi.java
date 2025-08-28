package com.ww.app.admin.user.rpc;

import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.admin.user.dto.SysUserDTO;
import com.ww.app.admin.user.fallback.AdminUserApiFallBack;
import com.ww.app.common.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2024-11-15- 11:26
 * @description: 后台用户api
 */
@Tag(name = "[RPC服务]-后台用户API")
@FeignClient(value = "ww-admin-manage", fallbackFactory = AdminUserApiFallBack.class)
public interface AdminUserApi {

    /**
     * 后台登录
     *
     * @param sysUserLoginBO bo
     * @return SysUserDTO
     */
    @Operation(summary = "后台系统登录")
    @PostMapping("/ww-admin-manage/admin/inner/adminLogin")
    Result<SysUserDTO> adminLogin(@RequestBody SysUserLoginBO sysUserLoginBO);

    /**
     * 【security】加载用户信息
     *
     * @param username username
     * @return SysUserDTO
     */
    @Operation(summary = "获取后台用户信息")
    @GetMapping("/ww-admin-manage/admin/inner/loadUserDetails")
    @Parameters({
            @Parameter(name = "username", description = "用户昵称", required = true, in = ParameterIn.QUERY),
    })
    Result<SysUserDTO> loadUserDetails(@RequestParam("username") String username);

}
