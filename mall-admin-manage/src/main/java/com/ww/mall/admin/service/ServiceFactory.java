package com.ww.mall.admin.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-21- 13:55
 * @description:
 */
@Getter
@Component
public class ServiceFactory {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private OperateLogService operateLogService;

}
