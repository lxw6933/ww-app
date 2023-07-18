package com.ww.mall.auth.controller;

import cn.hutool.core.util.RandomUtil;
import com.ww.mall.auth.feign.MemberFeignService;
import com.ww.mall.auth.feign.ThirdServerFeignService;
import com.ww.mall.auth.serivce.LoginService;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.web.view.bo.MemberLoginBO;
import com.ww.mall.auth.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 12:40
 **/
@Slf4j
@Validated
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ThirdServerFeignService thirdServerFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/loginByVerityCode")
    public LoginVO loginByVerityCode(@Validated @RequestBody MemberLoginBO memberLoginBO, HttpServletRequest request) {
        return loginService.loginByVerityCode(memberLoginBO, request);
    }

    @GetMapping("/sendCode")
    public void sendCode(@Pattern(regexp = "^1[3456789]\\d{9}$", message = "请输入正确的手机号码") @RequestParam("mobile") String mobile) {
        loginService.sendCode(mobile);
    }

}
