package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.form.ModifyPasswordForm;
import com.ww.mall.admin.view.form.SysUserForm;
import com.ww.mall.admin.view.query.SysUserPageQuery;
import com.ww.mall.admin.view.vo.CurrentSysUserInfoVO;
import com.ww.mall.admin.view.vo.SysUserVO;
import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
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

    @GetMapping("/user/page")
    public MallPageResult<SysUserVO> page(SysUserPageQuery query) {
        return sf.getSysUserService().page(query);
    }

    @GetMapping("/user")
    public SysUserVO info(@RequestParam("userId") Long userId) {
        return sf.getSysUserService().info(userId);
    }

    @PostMapping("/user")
    public boolean save(@RequestBody @Validated SysUserForm form) {
        return sf.getSysUserService().save(form);
    }

    @PutMapping("/user")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysUserForm form) {
        return sf.getSysUserService().update(form);
    }

    @DeleteMapping("/user")
    public boolean delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysUserService().delete(form);
    }

    @GetMapping("/user/self")
    public CurrentSysUserInfoVO selfInfo() {
        return sf.getSysUserService().selfInfo();
    }

    @PostMapping("/user/modifyPassword")
    public boolean modifyPassword(@RequestBody @Validated ModifyPasswordForm form) {
        return sf.getSysUserService().modifyPassword(form);
    }

    @GetMapping("/user/modifyStatus/{userId}")
    public boolean modifyStatus(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().modifyStatus(userId);
    }

    @GetMapping("/user/resetPassword/{userId}")
    public boolean resetPassword(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().resetPassword(userId);
    }

}

