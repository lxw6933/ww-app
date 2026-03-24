package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.MiddlewareConsoleVO;
import com.ww.app.ssh.service.MiddlewareConsoleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

/**
 * 中间件后台入口接口。
 * <p>
 * 提供测试环境下的中间件后台地址、账号密码展示，以及统一跳转能力。
 * 当前仅负责配置透出与跳转，不处理自动登录。
 * </p>
 */
@RestController
@RequestMapping("/api/middleware")
public class MiddlewareConsoleController {

    /**
     * 中间件后台查询服务。
     */
    private final MiddlewareConsoleService middlewareConsoleService;

    /**
     * 构造方法。
     *
     * @param middlewareConsoleService 中间件后台查询服务
     */
    public MiddlewareConsoleController(MiddlewareConsoleService middlewareConsoleService) {
        this.middlewareConsoleService = middlewareConsoleService;
    }

    /**
     * 查询指定实例的中间件后台列表。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 实例服务键
     * @return 中间件后台列表
     */
    @GetMapping("/consoles")
    public List<MiddlewareConsoleVO> listConsoles(@RequestParam("project") String project,
                                                  @RequestParam("env") String env,
                                                  @RequestParam("service") String service) {
        try {
            return middlewareConsoleService.listConsoles(project, env, service);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "查询中间件后台失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 跳转到指定中间件后台。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 实例服务键
     * @param code 中间件编码
     * @return 302 重定向响应
     */
    @GetMapping("/launch")
    public ResponseEntity<Void> launch(@RequestParam("project") String project,
                                       @RequestParam("env") String env,
                                       @RequestParam("service") String service,
                                       @RequestParam("code") String code) {
        try {
            String url = middlewareConsoleService.resolveLaunchUrl(project, env, service, code);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, URI.create(url).toASCIIString())
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "跳转中间件后台失败: " + ex.getMessage(), ex);
        }
    }
}
