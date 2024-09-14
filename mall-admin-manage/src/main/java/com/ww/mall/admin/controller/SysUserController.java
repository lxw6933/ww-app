package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.form.ModifyPasswordForm;
import com.ww.mall.admin.view.form.SysUserForm;
import com.ww.mall.admin.view.query.SysUserPageQuery;
import com.ww.mall.admin.view.vo.CurrentSysUserInfoVO;
import com.ww.mall.admin.view.vo.SysUserVO;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.web.view.form.IdForm;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController extends MallAbstractController {

    @GetMapping("/page")
    public MallPageResult<SysUserVO> page(SysUserPageQuery query) {
        return sf.getSysUserService().page(query);
    }

    @GetMapping("/selfInfo")
    public CurrentSysUserInfoVO selfInfo() {
        return sf.getSysUserService().selfInfo();
    }

    @GetMapping("/info/{userId}")
    public SysUserVO info(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().info(userId);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody @Validated SysUserForm form) {
        return sf.getSysUserService().save(form);
    }

    @PostMapping("/update")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysUserForm form) {
        return sf.getSysUserService().update(form);
    }

    @PostMapping("/delete")
    public boolean delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysUserService().delete(form);
    }

    @PostMapping("/modifyPassword")
    public boolean modifyPassword(@RequestBody @Validated ModifyPasswordForm form) {
        return sf.getSysUserService().modifyPassword(form);
    }

    @PostMapping("/modifyStatus/{userId}")
    public boolean modifyStatus(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().modifyStatus(userId);
    }

    @PostMapping("/resetPassword/{userId}")
    public boolean resetPassword(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().resetPassword(userId);
    }

}

