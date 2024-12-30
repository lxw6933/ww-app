package com.ww.app.admin.controller;

import com.ww.app.admin.view.form.ModifyPasswordForm;
import com.ww.app.admin.view.form.SysUserForm;
import com.ww.app.admin.view.query.SysUserPageQuery;
import com.ww.app.admin.view.vo.CurrentSysUserInfoVO;
import com.ww.app.admin.view.vo.SysUserVO;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.valid.group.DeleteGroup;
import com.ww.app.common.valid.group.UpdateGroup;
import com.ww.app.common.common.IdForm;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController extends AbstractController {

    @GetMapping("/user/page")
    public AppPageResult<SysUserVO> page(SysUserPageQuery query) {
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
    public boolean delete(@Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysUserService().delete(form);
    }

    @PermitAll
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

