package com.ww.mall.admin.operatelog;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.mzt.logapi.service.IParseFunction;
import com.ww.mall.admin.entity.SysRole;
import com.ww.mall.admin.service.SysRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-09-19- 14:31
 * @description:
 */
@Slf4j
@Component
public class RoleParseFunction implements IParseFunction {

    public static final String NAME = "getRoleById";

    @Resource
    private SysRoleService sysRoleService;

    @Override
    public String functionName() {
        return NAME;
    }

    @Override
    public String apply(Object value) {
        if (StrUtil.isEmptyIfStr(value)) {
            return "";
        }
        SysRole sysRole = sysRoleService.getById(Convert.toLong(value));
        if (sysRole == null) {
            log.warn("【apply】获取角色{{}}为空", value);
            return "";
        }
        return sysRole.getName();
    }
}
