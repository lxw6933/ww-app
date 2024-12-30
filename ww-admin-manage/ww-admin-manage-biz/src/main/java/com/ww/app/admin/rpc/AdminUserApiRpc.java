package com.ww.app.admin.rpc;

import com.ww.app.admin.controller.AbstractController;
import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.admin.user.dto.SysUserDTO;
import com.ww.app.admin.user.rpc.AdminUserApi;
import com.ww.app.common.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:16
 **/
@RestController
@RequestMapping("/admin/inner")
public class AdminUserApiRpc extends AbstractController implements AdminUserApi {

    @Override
    @PostMapping("/getAdminLoginUserInfo")
    public Result<SysUserDTO> adminLogin(SysUserLoginBO sysUserLoginBO) {
        return Result.success(sf.getSysUserService().login(sysUserLoginBO));
    }

    @Override
    @GetMapping("/loadUserDetails")
    public Result<SysUserDTO> loadUserDetails(String username) {
        return Result.success(sf.getSysUserService().loadUserDetails(username));
    }

}
