package com.ww.app.auth.controller;

import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.auth.serivce.ICaptchaService;
import com.ww.app.auth.serivce.LoginService;
import com.ww.app.auth.view.vo.LoginResultVO;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.ValidationUtils;
import com.ww.app.member.member.bo.MemberLoginBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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

    @Resource
    private LoginService loginService;
    @Resource
    private ICaptchaService captchaService;

    @PostMapping("/adminLogin")
    public LoginResultVO adminLogin(@RequestBody @Validated SysUserLoginBO sysUserLoginBO) {
        boolean validate = captchaService.validate(sysUserLoginBO.getUuid(), sysUserLoginBO.getCaptcha());
        if (!validate) {
            throw new ApiException("验证码不正确");
        }
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
