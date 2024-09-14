package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.form.SysRoleForm;
import com.ww.mall.admin.view.query.SysRolePageQuery;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.common.valid.group.UpdateGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@RestController
@RequestMapping("/sysRole")
public class SysRoleController extends MallAbstractController {

    @GetMapping("/page")
    public MallPageResult<SysRoleVO> page(SysRolePageQuery query) {
        return sf.getSysRoleService().page(query);
    }

    @GetMapping("/role")
    public SysRoleVO info(@RequestParam("roleId") Long roleId) {
        return sf.getSysRoleService().info(roleId);
    }

    @PostMapping("/role")
    public boolean save(@RequestBody @Validated SysRoleForm sysRoleForm) {
        return sf.getSysRoleService().save(sysRoleForm);
    }

    @PutMapping("/role")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysRoleForm sysRoleForm) {
        return sf.getSysRoleService().update(sysRoleForm);
    }

}

