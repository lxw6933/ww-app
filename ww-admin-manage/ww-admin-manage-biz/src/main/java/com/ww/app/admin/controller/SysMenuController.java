package com.ww.app.admin.controller;

import cn.hutool.core.lang.tree.Tree;
import com.ww.app.admin.enums.SysMenuType;
import com.ww.app.admin.view.form.SysMenuForm;
import com.ww.app.admin.view.vo.SysMenuParentVO;
import com.ww.app.admin.view.vo.SysMenuVO;
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
 * @create 2024-05-20 14:02:19
 * @description:
 */
@RestController
@RequestMapping("/sysMenu")
@Tag(name = "系统后台权限 API")
public class SysMenuController extends AbstractController {

    @GetMapping("/menu/tree")
    @Operation(summary = "所有菜单列表")
    public List<Tree<Long>> menuTree() {
        return sf.getSysMenuService().tree();
    }

    @GetMapping("/menu")
    @Operation(summary = "菜单详情")
    @Parameters({
            @Parameter(name = "menuId", description = "菜单权限id", required = true, in = ParameterIn.QUERY),
    })
    public SysMenuVO info(@RequestParam("menuId") Long menuId) {
        return sf.getSysMenuService().info(menuId);
    }

    @GetMapping("/menu/allParent")
    @Operation(summary = "上级菜单列表")
    @Parameters({
            @Parameter(name = "type", description = "菜单权限类型", required = true, in = ParameterIn.QUERY),
    })
    public List<SysMenuParentVO> allParent(@RequestParam("type") SysMenuType type) {
        return sf.getSysMenuService().allParent(type);
    }

    @PostMapping("/menu")
    @Operation(summary = "添加菜单权限")
    public boolean save(@RequestBody @Validated SysMenuForm form) {
        return sf.getSysMenuService().save(form);
    }

    @PutMapping("/menu")
    @Operation(summary = "更新菜单权限")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysMenuForm form) {
        return sf.getSysMenuService().update(form);
    }

    @DeleteMapping("/menu")
    @Operation(summary = "删除菜单权限")
    public boolean delete(@Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysMenuService().delete(form);
    }

}

