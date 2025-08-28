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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "系统后台用户 API")
public class SysUserController extends AbstractController {

    @GetMapping("/user/page")
    @Operation(summary = "用户列表")
    public AppPageResult<SysUserVO> page(SysUserPageQuery query) {
        return sf.getSysUserService().page(query);
    }

    @GetMapping("/user")
    @Operation(summary = "查看用户信息")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.QUERY),
    })
    public SysUserVO info(@RequestParam("userId") Long userId) {
        return sf.getSysUserService().info(userId);
    }

    @PostMapping("/user")
    @Operation(summary = "新增用户")
    public boolean save(@RequestBody @Validated SysUserForm form) {
        return sf.getSysUserService().save(form);
    }

    @PutMapping("/user")
    @Operation(summary = "修改用户")
    public boolean update(@RequestBody @Validated({UpdateGroup.class}) SysUserForm form) {
        return sf.getSysUserService().update(form);
    }

    @DeleteMapping("/user")
    @Operation(summary = "删除用户")
    public boolean delete(@Validated({DeleteGroup.class}) IdForm form) {
        return sf.getSysUserService().delete(form);
    }

    @PermitAll
    @GetMapping("/user/self")
    @Operation(summary = "获取当前登录用户信息")
    public CurrentSysUserInfoVO selfInfo() {
        return sf.getSysUserService().selfInfo();
    }

    @PostMapping("/user/modifyPassword")
    @Operation(summary = "修改密码")
    public boolean modifyPassword(@RequestBody @Validated ModifyPasswordForm form) {
        return sf.getSysUserService().modifyPassword(form);
    }

    @GetMapping("/user/modifyStatus/{userId}")
    @Operation(summary = "修改用户状态")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.PATH),
    })
    public boolean modifyStatus(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().modifyStatus(userId);
    }

    @GetMapping("/user/resetPassword/{userId}")
    @Operation(summary = "重置密码")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.PATH),
    })
    public boolean resetPassword(@PathVariable("userId") Long userId) {
        return sf.getSysUserService().resetPassword(userId);
    }

}

