package com.ww.mall.admin.framework.operatelog;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.mzt.logapi.service.IParseFunction;
import com.ww.mall.admin.entity.SysMenu;
import com.ww.mall.admin.service.SysMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-09-19- 14:59
 * @description:
 */
@Slf4j
@Component
public class MenuParseFunction implements IParseFunction {

    public static final String NAME = "getMenuById";

    @Resource
    private SysMenuService sysMenuService;

    @Override
    public String functionName() {
        return NAME;
    }

    @Override
    public String apply(Object value) {
        if (StrUtil.isEmptyIfStr(value)) {
            return "";
        }
        SysMenu sysMenu = sysMenuService.getById(Convert.toLong(value));
        if (sysMenu == null) {
            log.warn("【apply】获取menu{{}}为空", value);
            return "";
        }
        return sysMenu.getName();
    }
}
