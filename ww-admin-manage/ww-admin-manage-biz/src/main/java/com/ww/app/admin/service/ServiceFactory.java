package com.ww.app.admin.service;

import com.ww.app.admin.component.key.AuthorityRedisKeyBuilder;
import com.ww.app.operatelog.core.service.OperateLogService;
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
    private AuthorityRedisKeyBuilder authorityRedisKeyBuilder;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private OperateLogService operateLogService;

}
