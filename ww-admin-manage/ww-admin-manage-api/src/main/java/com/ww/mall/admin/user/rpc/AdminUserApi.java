package com.ww.mall.admin.user.rpc;

import com.ww.mall.admin.user.bo.SysUserLoginBO;
import com.ww.mall.admin.user.dto.SysUserDTO;
import com.ww.mall.admin.user.fallback.AdminUserApiFallBack;
import com.ww.mall.common.common.Result;
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
@FeignClient(value = "ww-admin-manage", fallbackFactory = AdminUserApiFallBack.class)
public interface AdminUserApi {

    /**
     * 后台登录
     *
     * @param sysUserLoginBO bo
     * @return SysUserDTO
     */
    @PostMapping("/ww-admin-manage/admin/inner/adminLogin")
    Result<SysUserDTO> adminLogin(@RequestBody SysUserLoginBO sysUserLoginBO);

    /**
     * 【security】加载用户信息
     *
     * @param username username
     * @return SysUserDTO
     */
    @GetMapping("/ww-admin-manage/admin/inner/loadUserDetails")
    Result<SysUserDTO> loadUserDetails(@RequestParam("username") String username);

}
