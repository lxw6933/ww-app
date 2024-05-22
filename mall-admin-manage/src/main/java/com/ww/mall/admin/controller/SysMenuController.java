package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.form.SysMenuForm;
import com.ww.mall.admin.view.vo.SysMenuVO;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.web.view.form.IdForm;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ww
 * @create 2024-05-20 14:02:19
 * @description:
 */
@RestController
@RequestMapping("/sysMenu")
public class SysMenuController extends MallAbstractController {

    @GetMapping("/info/{menuId}")
    public SysMenuVO info(@PathVariable("menuId") Long menuId) {
        return sf.getSysMenuService().info(menuId);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody @Validated SysMenuForm form) {
        return sf.getSysMenuService().save(form);
    }

    @PostMapping("/update")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysMenuForm form) {
        return sf.getSysMenuService().update(form);
    }

    @PostMapping("/delete")
    public boolean delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysMenuService().delete(form);
    }

}

