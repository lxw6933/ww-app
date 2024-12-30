package com.ww.app.auth.controller;

import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.auth.serivce.LoginService;
import com.ww.app.auth.view.vo.LoginResultVO;
import com.ww.app.common.utils.ValidationUtils;
import com.ww.app.member.member.bo.MemberLoginBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Pattern;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 12:40
 **/
@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/adminLogin")
    public LoginResultVO adminLogin(@RequestBody @Validated SysUserLoginBO sysUserLoginBO) {
        return loginService.adminLogin(sysUserLoginBO);
    }

    @PostMapping("/clientMobileLogin")
    public LoginResultVO clientMobileLogin(@RequestBody @Validated MemberLoginBO memberLoginBO) {
        return loginService.clientMobileLogin(memberLoginBO);
    }

    @GetMapping("/sendCode")
    public void sendCode(@Pattern(regexp = ValidationUtils.MOBILE_REG, message = "请输入正确的手机号码") @RequestParam("mobile") String mobile) {
        loginService.sendCode(mobile);
    }

}
