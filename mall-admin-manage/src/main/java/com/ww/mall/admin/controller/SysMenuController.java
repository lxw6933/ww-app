package com.ww.mall.admin.controller;

import cn.hutool.core.lang.tree.Tree;
import com.ww.mall.admin.enums.SysMenuType;
import com.ww.mall.admin.view.form.SysMenuForm;
import com.ww.mall.admin.view.vo.SysMenuParentVO;
import com.ww.mall.admin.view.vo.SysMenuVO;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.web.view.form.IdForm;
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
public class SysMenuController extends MallAbstractController {

    @GetMapping("/menu/tree")
    public List<Tree<Long>> menuTree() {
        return sf.getSysMenuService().tree();
    }

    @GetMapping("/menu")
    public SysMenuVO info(@RequestParam("menuId") Long menuId) {
        return sf.getSysMenuService().info(menuId);
    }

    @GetMapping("/menu/allParent")
    public List<SysMenuParentVO> allParent(@RequestParam("type") SysMenuType type) {
        return sf.getSysMenuService().allParent(type);
    }

    @PostMapping("/menu")
    public boolean save(@RequestBody @Validated SysMenuForm form) {
        return sf.getSysMenuService().save(form);
    }

    @PutMapping("/menu")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysMenuForm form) {
        return sf.getSysMenuService().update(form);
    }

    @DeleteMapping("/menu")
    public boolean delete(@Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysMenuService().delete(form);
    }

}

