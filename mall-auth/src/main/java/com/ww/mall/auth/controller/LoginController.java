package com.ww.mall.auth.controller;

import com.ww.mall.auth.serivce.LoginService;
import com.ww.mall.auth.view.vo.AdminLoginResultVO;
import com.ww.mall.auth.view.vo.LoginResultVO;
import com.ww.mall.web.view.bo.MemberLoginBO;
import com.ww.mall.web.view.bo.SysUserLoginBO;
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
    public AdminLoginResultVO adminLogin(@RequestBody @Validated SysUserLoginBO sysUserLoginBO) {
        return loginService.adminLogin(sysUserLoginBO);
    }

    @PostMapping("/clientMobileLogin")
    public LoginResultVO clientMobileLogin(@RequestBody @Validated MemberLoginBO memberLoginBO) {
        return loginService.clientMobileLogin(memberLoginBO);
    }

    @GetMapping("/sendCode")
    public void sendCode(@Pattern(regexp = "^1[3456789]\\d{9}$", message = "请输入正确的手机号码") @RequestParam("mobile") String mobile) {
        loginService.sendCode(mobile);
    }

}
