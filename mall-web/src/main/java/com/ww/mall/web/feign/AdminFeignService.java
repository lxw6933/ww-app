package com.ww.mall.web.feign;

import com.ww.mall.common.common.Result;
import com.ww.mall.web.feign.inner.AdminFeignServiceFallBack;
import com.ww.mall.web.view.bo.SysUserLoginBO;
import com.ww.mall.web.view.dto.SysUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2023-07-18- 10:31
 * @description:
 */
@FeignClient(value = "mall-admin-manage", fallbackFactory = AdminFeignServiceFallBack.class)
public interface AdminFeignService {

    /**
     * 后台登录
     *
     * @param sysUserLoginBO bo
     * @return SysUserDTO
     */
    @PostMapping("/mall-admin-manage/admin/inner/login")
    Result<SysUserDTO> login(@RequestBody SysUserLoginBO sysUserLoginBO);

    /**
     * 【security】加载用户信息
     *
     * @param username username
     * @return SysUserDTO
     */
    @PostMapping("/mall-admin-manage/admin/inner/loadUserDetails")
    Result<SysUserDTO> loadUserDetails(@RequestParam("username") String username);

}
