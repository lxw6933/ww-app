package com.ww.mall.admin.controller;

import com.ww.mall.admin.service.SysMenuService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-20 14:02:19
 * @description:
 */
@RestController
@RequestMapping("sys_menu")
public class SysMenuController {

    @Resource
    private SysMenuService sysMenuService;

}

