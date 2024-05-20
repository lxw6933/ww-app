package com.ww.mall.admin.controller;

import com.ww.mall.admin.service.SysRoleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@RestController
@RequestMapping("sys_role")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

}

