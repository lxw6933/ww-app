package com.ww.app.admin.controller;

import cn.hutool.core.lang.tree.Tree;
import com.ww.app.admin.enums.SysMenuType;
import com.ww.app.admin.view.form.NameExistsForm;
import com.ww.app.admin.view.form.PathExistsForm;
import com.ww.app.admin.view.form.SysMenuForm;
import com.ww.app.admin.view.vo.SysMenuParentVO;
import com.ww.app.admin.view.vo.SysMenuVO;
import com.ww.app.common.common.IdForm;
import com.ww.app.common.valid.group.DeleteGroup;
import com.ww.app.common.valid.group.UpdateGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "名称校验")
    @PostMapping("/menu/name-exists")
    public boolean nameExists(@RequestBody @Validated NameExistsForm form) {
        return sf.getSysMenuService().nameExists(form.getId(), form.getName());
    }

    @Operation(summary = "路径校验")
    @PostMapping("/menu/path-exists")
    public boolean pathExists(@RequestBody @Validated PathExistsForm form) {
        return sf.getSysMenuService().pathExists(form.getId(), form.getPath());
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

