package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 日志面板配置查询接口。
 * <p>
 * 向前端提供环境、服务和日志文件候选列表。
 * </p>
 */
@RestController
@RequestMapping("/api/config")
public class LogPanelConfigController {

    /**
     * 配置查询服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * SSH 日志服务。
     */
    private final SshLogService sshLogService;

    /**
     * 构造方法。
     *
     * @param logPanelQueryService 配置查询服务
     * @param sshLogService        SSH 日志服务
     */
    public LogPanelConfigController(LogPanelQueryService logPanelQueryService, SshLogService sshLogService) {
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    /**
     * 查询全部环境服务概览。
     *
     * @return 环境服务概览
     */
    @GetMapping("/servers")
    public Map<String, Map<String, Map<String, String>>> listServers() {
        return logPanelQueryService.getServerOverview();
    }

    /**
     * 查询指定环境与服务的日志文件候选列表。
     *
     * @param env     环境名称
     * @param service 服务名称
     * @return 日志文件路径集合
     */
    @GetMapping("/files")
    public List<String> listFiles(@RequestParam("env") String env, @RequestParam("service") String service) {
        try {
            LogTarget target = logPanelQueryService.resolveTarget(env, service);
            return sshLogService.listLogFiles(target);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "查询日志文件失败: " + ex.getMessage(), ex);
        }
    }
}
