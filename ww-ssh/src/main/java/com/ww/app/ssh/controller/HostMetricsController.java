package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.MetricsActiveRegisterRequest;
import com.ww.app.ssh.model.MetricsSnapshotResponse;
import com.ww.app.ssh.service.MetricsSnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 主机指标查询接口。
 * <p>
 * 当前接口职责拆分如下：
 * 1. 活跃维度登记接口：由前端告知当前正在浏览哪个项目/环境/服务；<br>
 * 2. 快照读取接口：仅返回后端本地缓存中的指标结果，不再同步触发 SSH 采集。<br>
 * </p>
 */
@RestController
@RequestMapping("/api/metrics")
public class HostMetricsController {

    /**
     * 指标快照服务。
     */
    private final MetricsSnapshotService metricsSnapshotService;

    /**
     * 构造方法。
     *
     * @param metricsSnapshotService 指标快照服务
     */
    public HostMetricsController(MetricsSnapshotService metricsSnapshotService) {
        this.metricsSnapshotService = metricsSnapshotService;
    }

    /**
     * 注册当前页面正在浏览的指标维度。
     * <p>
     * 仅做本地活跃登记，不在请求线程内同步采集 SSH 指标。
     * </p>
     *
     * @param request 注册请求
     */
    @PostMapping("/active/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerActiveMetrics(@RequestBody MetricsActiveRegisterRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("请求体不能为空");
            }
            metricsSnapshotService.registerActiveKey(
                    request.normalizedProject(),
                    request.normalizedEnv(),
                    request.normalizedService(),
                    request.normalizedSource()
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "注册活跃指标维度失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 查询主机指标列表。
     * <p>
     * 该接口只读取本地缓存；若首次注册后调度尚未完成刷新，则返回空列表，
     * 由前端展示“指标准备中”状态并等待下一轮自动刷新。
     * </p>
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 服务名称
     * @return 主机指标快照响应
     */
    @GetMapping("/hosts")
    public MetricsSnapshotResponse listHostMetrics(@RequestParam("project") String project,
                                                   @RequestParam("env") String env,
                                                   @RequestParam("service") String service) {
        try {
            return metricsSnapshotService.readSnapshot(project, env, service);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "查询主机指标失败: " + ex.getMessage(), ex);
        }
    }
}
