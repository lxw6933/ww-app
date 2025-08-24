package com.ww.app.auth.controller;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.ww.app.captcha.core.easy.EasyCaptchaService;
import com.ww.app.captcha.core.easy.vo.EasyCaptchaVO;
import com.ww.app.common.utils.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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

     @Resource
     private CaptchaService captchaService;

     @PostMapping({"/get"})
     @Operation(summary = "获得图像验证码")
     public ResponseModel get(@RequestBody CaptchaVO data, HttpServletRequest request) {
         data.setBrowserInfo(getRemoteId(request));
         return captchaService.get(data);
     }

     @PostMapping("/check")
     @Operation(summary = "校验图像验证码")
     public ResponseModel check(@RequestBody CaptchaVO data, HttpServletRequest request) {
         data.setBrowserInfo(getRemoteId(request));
         return captchaService.check(data);
     }

    public static String getRemoteId(HttpServletRequest request) {
        String ip = IpUtil.getRealIp(request);
        String ua = request.getHeader("user-agent");
        if (StrUtil.isNotBlank(ip)) {
            return ip + ua;
        }
        return request.getRemoteAddr() + ua;
    }

    @Resource
    private EasyCaptchaService easyCaptchaService;

    @GetMapping("image")
    @Operation(summary = "获取验证码图片", description = "获取验证码图片")
    public EasyCaptchaVO image() {
        return easyCaptchaService.image(170, 60);
    }

}
