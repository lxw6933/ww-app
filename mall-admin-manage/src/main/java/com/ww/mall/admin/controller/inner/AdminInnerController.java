package com.ww.mall.admin.controller.inner;

import com.ww.mall.admin.controller.MallAbstractController;
import com.ww.mall.web.view.bo.SysUserLoginBO;
import com.ww.mall.web.view.dto.SysUserDTO;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:16
 **/
@RestController
@RequestMapping("/admin/inner")
public class AdminInnerController extends MallAbstractController {

    @GetMapping("/login")
    public SysUserDTO login(@RequestBody SysUserLoginBO sysUserLoginBO) {
        return sf.getSysUserService().login(sysUserLoginBO);
    }

}
