package com.ww.app.admin.controller;

import com.ww.app.admin.view.form.SysRoleForm;
import com.ww.app.admin.view.query.SysRolePageQuery;
import com.ww.app.admin.view.vo.SysRoleSelectVO;
import com.ww.app.admin.view.vo.SysRoleVO;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.valid.group.DeleteGroup;
import com.ww.app.common.valid.group.UpdateGroup;
import com.ww.app.common.common.IdForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "系统后台角色 API")
public class SysRoleController extends AbstractController {

    @GetMapping("/role/page")
    @Operation(summary = "角色列表")
    public AppPageResult<SysRoleVO> page(SysRolePageQuery query) {
        return sf.getSysRoleService().page(query);
    }

    @GetMapping("/role")
    @Operation(summary = "查看角色信息")
    @Parameters({
            @Parameter(name = "roleId", description = "角色id", required = true, in = ParameterIn.QUERY),
    })
    public SysRoleVO info(@RequestParam("roleId") Long roleId) {
        return sf.getSysRoleService().info(roleId);
    }

    @GetMapping("/role/all")
    @Operation(summary = "获取所有角色")
    public List<SysRoleSelectVO> all() {
        return sf.getSysRoleService().getAllRole();
    }

    @PostMapping("/role/modifyStatus/{roleId}")
    @Operation(summary = "修改角色状态")
    @Parameters({
            @Parameter(name = "roleId", description = "角色id", required = true, in = ParameterIn.PATH),
    })
    public boolean modifyStatus(@PathVariable("roleId") Long roleId) {
        return sf.getSysRoleService().modifyStatus(roleId);
    }

    @PostMapping("/role")
    @Operation(summary = "新增角色")
    public boolean save(@RequestBody @Validated SysRoleForm sysRoleForm) {
        return sf.getSysRoleService().save(sysRoleForm);
    }

    @PutMapping("/role")
    @Operation(summary = "更新角色")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysRoleForm sysRoleForm) {
        return sf.getSysRoleService().update(sysRoleForm);
    }

    @DeleteMapping("/role")
    @Operation(summary = "删除角色")
    public boolean delete(@Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysRoleService().delete(form);
    }

}

