package com.ww.mall.admin.controller;

import com.ww.mall.admin.service.SysUserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@RestController
@RequestMapping("sys_user")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

}

