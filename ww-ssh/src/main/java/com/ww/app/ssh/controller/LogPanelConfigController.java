package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * 日志组件。
     */
    private static final Logger log = LoggerFactory.getLogger(LogPanelConfigController.class);

    /**
     * 文件发现部分失败数量响应头。
     */
    private static final String HEADER_PARTIAL_FAILURE_COUNT = "X-WW-Partial-Failure-Count";

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
    public Map<String, Map<String, Map<String, Map<String, String>>>> listServers() {
        return logPanelQueryService.getServerOverview();
    }

    /**
     * 查询指定环境与服务的日志文件候选列表。
     *
     * @param project 项目名称
     * @param env     环境名称
     * @param service 服务名称
     * @return 日志文件路径集合
     */
    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(@RequestParam("project") String project,
                                                  @RequestParam("env") String env,
                                                  @RequestParam("service") String service) {
        try {
            List<LogTarget> targets = logPanelQueryService.resolveTargets(project, env, service);
            Set<String> fileSet = new LinkedHashSet<>();
            int successCount = 0;
            int failedCount = 0;
            for (LogTarget target : targets) {
                try {
                    fileSet.addAll(sshLogService.listLogFiles(target));
                    successCount++;
                } catch (Exception ex) {
                    failedCount++;
                    log.warn("日志文件发现失败: target={}, error={}",
                            target == null ? "unknown" : target.displayName(),
                            ex.getMessage());
                }
            }
            if (successCount == 0) {
                throw new IllegalStateException("目标实例均不可用，无法获取日志文件列表");
            }
            HttpHeaders headers = new HttpHeaders();
            if (failedCount > 0) {
                headers.add(HEADER_PARTIAL_FAILURE_COUNT, String.valueOf(failedCount));
            }
            return ResponseEntity.ok().headers(headers).body(new ArrayList<>(fileSet));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "查询日志文件失败: " + ex.getMessage(), ex);
        }
    }
}
