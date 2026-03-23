package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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
    public ResponseEntity<List<String>> readByCat(@RequestBody LogStreamRequest request) {
        try {
            validateFilePathPolicy(request);
            List<LogTarget> targets = logPanelQueryService.resolveTargets(request);
            int keep = request.normalizedLines();
            boolean aggregateTailWindow = request.isAllService();
            List<String> rows = aggregateTailWindow ? null : new ArrayList<>();
            Deque<String> tailWindow = aggregateTailWindow ? new ArrayDeque<>(keep) : null;
            for (LogTarget target : targets) {
                try {
                    List<String> targetRows = sshLogService.readByCat(target, request);
                    if (aggregateTailWindow) {
                        appendTailWindow(tailWindow, targetRows, keep);
                    } else {
                        rows.addAll(targetRows);
                    }
                } catch (Exception ex) {
                    String errorLine = "[系统提示] 读取失败 " + target.displayName() + ": " + ex.getMessage();
                    if (aggregateTailWindow) {
                        appendTailWindow(tailWindow, java.util.Collections.singletonList(errorLine), keep);
                    } else {
                        rows.add(errorLine);
                    }
                }
            }
            // 说明：
            // - 单服务/单文件：每个目标内部已按 requested lines 做尾部窗口裁剪，直接返回即可；
            // - 全部服务聚合：若直接拼接所有目标窗口，页面会混入大量“很久没产生日志”的实例尾巴，
            //   视觉上会表现为“cat 查到的都是旧日志”。因此聚合场景保留“全局取最新 N 行”行为。
            if (aggregateTailWindow) {
                rows = new ArrayList<>(tailWindow);
            }
            HttpHeaders headers = new HttpHeaders();
            // 明确告知客户端/代理不要缓存，避免出现“cat 总是旧数据”的误判。
            // 兼容部分代理对 POST 的异常缓存行为。
            headers.setCacheControl(CacheControl.noStore().mustRevalidate().getHeaderValue());
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            return ResponseEntity.ok().headers(headers).body(rows);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "日志快照读取失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 校验日志文件选择策略。
     * <p>
     * 单服务模式下要求前端显式传入 filePath，避免后端自动回退默认文件导致排查对象不明确。
     * </p>
     *
     * @param request 请求参数
     */
    private void validateFilePathPolicy(LogStreamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!request.isAllService() && request.normalizedFilePath().isEmpty()) {
            throw new IllegalArgumentException("单服务模式下必须显式选择日志文件");
        }
    }

    /**
     * 向“全部服务”聚合尾窗追加日志行。
     * <p>
     * 聚合场景仅保留全局最新 N 行，避免先把所有目标结果完整堆入内存后再裁剪。
     * </p>
     *
     * @param tailWindow 全局尾窗
     * @param rows       待追加日志行
     * @param keep       最大保留行数
     */
    private void appendTailWindow(Deque<String> tailWindow, List<String> rows, int keep) {
        if (tailWindow == null || rows == null || rows.isEmpty() || keep <= 0) {
            return;
        }
        for (String row : rows) {
            if (tailWindow.size() >= keep) {
                tailWindow.pollFirst();
            }
            tailWindow.addLast(row);
        }
    }
}
