package com.ww.app.auth.controller;

import com.ww.app.auth.serivce.ICaptchaService;
import com.ww.app.auth.view.vo.CaptchaResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-06-18- 17:17
 * @description:
 */
@Slf4j
@RestController
@Tag(name = "验证码 API")
@RequestMapping("/captcha")
public class CaptchaController {

    // @Resource
    // private CaptchaService captchaService;
    @Resource
    private ICaptchaService captchaService;

    // @PostMapping({"/get"})
    // @Operation(summary = "获得验证码")
    // public ResponseModel get(@RequestBody CaptchaVO data, HttpServletRequest request) {
    //     data.setBrowserInfo(getRemoteId(request));
    //     return captchaService.get(data);
    // }
    //
    // @PostMapping("/check")
    // @Operation(summary = "校验验证码")
    // public ResponseModel check(@RequestBody CaptchaVO data, HttpServletRequest request) {
    //     data.setBrowserInfo(getRemoteId(request));
    //     return captchaService.check(data);
    // }

    @GetMapping("image")
    @Operation(summary = "获取验证码图片", description = "获取验证码图片")
    public CaptchaResp image() {
        return captchaService.image(170, 60);
    }

    // public static String getRemoteId(HttpServletRequest request) {
    //     String ip = IpUtil.getRealIp(request);
    //     String ua = request.getHeader("user-agent");
    //     if (StrUtil.isNotBlank(ip)) {
    //         return ip + ua;
    //     }
    //     return request.getRemoteAddr() + ua;
    // }

}
