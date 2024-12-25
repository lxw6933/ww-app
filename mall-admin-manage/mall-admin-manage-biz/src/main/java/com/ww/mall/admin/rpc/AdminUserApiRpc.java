package com.ww.mall.admin.rpc;

import com.ww.mall.admin.controller.MallAbstractController;
import com.ww.mall.admin.user.bo.SysUserLoginBO;
import com.ww.mall.admin.user.dto.SysUserDTO;
import com.ww.mall.admin.user.rpc.AdminUserApi;
import com.ww.mall.common.common.Result;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:16
 **/
@RestController
public class AdminUserApiRpc extends MallAbstractController implements AdminUserApi {

    @Override
    public Result<SysUserDTO> adminLogin(SysUserLoginBO sysUserLoginBO) {
        return Result.success(sf.getSysUserService().login(sysUserLoginBO));
    }

    @Override
    public Result<SysUserDTO> loadUserDetails(String username) {
        return Result.success(sf.getSysUserService().loadUserDetails(username));
    }

}
