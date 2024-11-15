package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.form.SysRoleForm;
import com.ww.mall.admin.view.query.SysRolePageQuery;
import com.ww.mall.admin.view.vo.SysRoleSelectVO;
import com.ww.mall.admin.view.vo.SysRoleVO;
import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.common.common.IdForm;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@RestController
@RequestMapping("/sysRole")
public class SysRoleController extends MallAbstractController {

    @GetMapping("/role/page")
    public MallPageResult<SysRoleVO> page(SysRolePageQuery query) {
        return sf.getSysRoleService().page(query);
    }

    @GetMapping("/role")
    public SysRoleVO info(@RequestParam("roleId") Long roleId) {
        return sf.getSysRoleService().info(roleId);
    }

    @GetMapping("/role/all")
    public List<SysRoleSelectVO> all() {
        return sf.getSysRoleService().getAllRole();
    }

    @GetMapping("/role/modifyStatus/{roleId}")
    public boolean modifyStatus(@PathVariable("roleId") Long roleId) {
        return sf.getSysRoleService().modifyStatus(roleId);
    }

    @PostMapping("/role")
    public boolean save(@RequestBody @Validated SysRoleForm sysRoleForm) {
        return sf.getSysRoleService().save(sysRoleForm);
    }

    @PutMapping("/role")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysRoleForm sysRoleForm) {
        return sf.getSysRoleService().update(sysRoleForm);
    }

    @DeleteMapping("/role")
    public boolean delete(@Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysRoleService().delete(form);
    }

}

