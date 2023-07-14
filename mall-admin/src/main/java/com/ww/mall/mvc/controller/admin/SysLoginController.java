package com.ww.mall.mvc.controller.admin;

import com.ww.mall.annotation.SysLog;
import com.ww.mall.config.security.service.JwtAuthService;
import com.ww.mall.enums.Action;
import com.ww.mall.mvc.service.SysUserService;
import com.ww.mall.mvc.view.form.LoginForm;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 后台登录
 * @author: ww
 * @create: 2021/6/26 上午9:23
 **/
@RestController
@RequestMapping("/admin/sys/login")
public class SysLoginController extends AbstractController {

    @Resource
    private JwtAuthService jwtAuthService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登陆认证
     *
     * @param form 登录表单
     * @return R
     */
    @PostMapping("/authentication")
    @SysLog(value = "后台用户登录", action = Action.LOGIN)
    public R login(@RequestBody @Validated LoginForm form) {
        String username = form.getUsername();
        String password = form.getPassword();
        String token = "";
        try {
            // 登录验证（成功：生成token）
            token = jwtAuthService.login(username, password);
        } catch (LockedException e) {
            e.printStackTrace();
            return R.error("账号已冻结");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        // TODO 异步更新用户登录时间
        return R.ok(token);
    }

    /**
     * 刷新token
     *
     * @param oldToken 旧token
     * @return newToken
     */
    @RequestMapping("/refreshToken")
    public R refreshToken(@RequestHeader String oldToken) throws Exception {
        return R.ok(jwtAuthService.refreshToken(oldToken));
    }

}
