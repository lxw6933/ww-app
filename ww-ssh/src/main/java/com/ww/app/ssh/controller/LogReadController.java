package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志读取接口。
 * <p>
 * 提供一次性快照读取能力，适用于“cat 模式”快速排查。
 * </p>
 */
@RestController
@RequestMapping("/api/log")
public class LogReadController {

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
    public LogReadController(LogPanelQueryService logPanelQueryService, SshLogService sshLogService) {
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    /**
     * 一次性读取日志快照（cat 模式）。
     *
     * @param request 日志请求参数
     * @return 日志文本行集合
     */
    @PostMapping("/cat")
    public List<String> readByCat(@RequestBody LogStreamRequest request) {
        try {
            List<LogTarget> targets = logPanelQueryService.resolveTargets(request);
            List<String> rows = new ArrayList<>();
            for (LogTarget target : targets) {
                try {
                    rows.addAll(sshLogService.readByCat(target, request));
                } catch (Exception ex) {
                    rows.add("[系统提示] 读取失败 " + target.displayName() + ": " + ex.getMessage());
                }
            }
            int keep = request.normalizedLines();
            if (rows.size() <= keep) {
                return rows;
            }
            return new ArrayList<>(rows.subList(rows.size() - keep, rows.size()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "日志快照读取失败: " + ex.getMessage(), ex);
        }
    }
}
