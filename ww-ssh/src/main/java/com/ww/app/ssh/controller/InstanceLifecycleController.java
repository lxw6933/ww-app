package com.ww.app.ssh.controller;

import com.ww.app.common.utils.IpUtil;
import com.ww.app.ssh.model.InstanceOperationRequest;
import com.ww.app.ssh.model.InstanceOperationResponse;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实例生命周期运维接口。
 * <p>
 * 提供对单实例的启动、重启、停止操作能力。
 * </p>
 */
@RestController
@RequestMapping("/api/instance")
public class InstanceLifecycleController {

    /**
     * 日志组件。
     */
    private static final Logger log = LoggerFactory.getLogger(InstanceLifecycleController.class);

    /**
     * 日志事件名称。
     */
    private static final String EVENT_INSTANCE_OPERATION = "instance-operation";

    /**
     * 操作日志阶段：收到请求。
     */
    private static final String STAGE_ATTEMPT = "attempt";

    /**
     * 操作日志阶段：参数无效。
     */
    private static final String STAGE_INVALID_PARAMS = "invalid-params";

    /**
     * 操作日志阶段：锁竞争失败。
     */
    private static final String STAGE_LOCK_FAILED = "lock-failed";

    /**
     * 操作日志阶段：执行成功。
     */
    private static final String STAGE_SUCCESS = "success";

    /**
     * 操作日志阶段：执行失败。
     */
    private static final String STAGE_FAILED = "failed";

    /**
     * 运维结果摘要最大日志长度。
     */
    private static final int RESULT_MAX_LOG_LEN = 160;

    /**
     * 配置解析服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * SSH 日志服务（复用 SSH 执行能力）。
     */
    private final SshLogService sshLogService;

    /**
     * 实例运维并发锁。
     * <p>
     * 锁粒度为 project+env+service，防止同一实例并发执行多个运维动作。
     * </p>
     */
    private final Map<String, ReentrantLock> operationLocks = new ConcurrentHashMap<>();

    /**
     * 构造方法。
     *
     * @param logPanelQueryService 配置解析服务
     * @param sshLogService        SSH 日志服务
     */
    public InstanceLifecycleController(LogPanelQueryService logPanelQueryService, SshLogService sshLogService) {
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    /**
     * 执行实例运维动作。
     *
     * @param request 运维请求
     * @param httpRequest HTTP 请求（用于解析来源 IP）
     * @return 操作响应
     */
    @PostMapping("/operate")
    public InstanceOperationResponse operate(@RequestBody InstanceOperationRequest request,
                                             HttpServletRequest httpRequest) {
        long startAt = System.currentTimeMillis();
        String clientIp = IpUtil.getRealIp(httpRequest);
        String project = request == null ? "" : request.normalizedProject();
        String env = request == null ? "" : request.normalizedEnv();
        String service = request == null ? "" : request.normalizedService();
        String action = request == null ? "" : request.normalizedAction();
        log.info("event={} stage={} ip={} project={} env={} service={} action={}",
                EVENT_INSTANCE_OPERATION, STAGE_ATTEMPT, clientIp, project, env, service, action);
        if (project.isEmpty() || env.isEmpty() || service.isEmpty() || action.isEmpty()) {
            log.warn("event={} stage={} ip={} project={} env={} service={} action={}",
                    EVENT_INSTANCE_OPERATION, STAGE_INVALID_PARAMS, clientIp, project, env, service, action);
            return InstanceOperationResponse.failure(project, env, service, action,
                    "参数不完整：project/env/service/action 必填");
        }
        String lockKey = buildOperationLockKey(project, env, service);
        ReentrantLock operationLock = operationLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        boolean locked = false;
        try {
            locked = tryAcquireOperationLock(operationLock);
            if (!locked) {
                log.warn("event={} stage={} ip={} project={} env={} service={} action={} costMs={}",
                        EVENT_INSTANCE_OPERATION, STAGE_LOCK_FAILED, clientIp, project, env, service, action,
                        System.currentTimeMillis() - startAt);
                return InstanceOperationResponse.failure(project, env, service, action,
                        "当前实例有运维操作执行中，请稍后重试");
            }
            LogTarget target = logPanelQueryService.resolveExactTarget(project, env, service);
            String output = sshLogService.operateInstance(target, action);
            log.info("event={} stage={} ip={} project={} env={} service={} action={} host={} costMs={} output={}",
                    EVENT_INSTANCE_OPERATION, STAGE_SUCCESS, clientIp, project, env, service, action,
                    resolveTargetHost(target), System.currentTimeMillis() - startAt, summarizeOutputForLog(output));
            return InstanceOperationResponse.success(project, env, service, action,
                    summarizeMessage(action, output), output);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "实例运维失败" : ex.getMessage();
            log.warn("event={} stage={} ip={} project={} env={} service={} action={} costMs={} error={}",
                    EVENT_INSTANCE_OPERATION, STAGE_FAILED, clientIp, project, env, service, action,
                    System.currentTimeMillis() - startAt, summarizeOutputForLog(message));
            return InstanceOperationResponse.failure(project, env, service, action, message, message);
        } finally {
            if (locked) {
                operationLock.unlock();
            }
            if (!operationLock.isLocked() && !operationLock.hasQueuedThreads()) {
                operationLocks.remove(lockKey, operationLock);
            }
        }
    }

    /**
     * 构建实例运维锁键。
     *
     * @param project 项目名
     * @param env     环境名
     * @param service 实例服务键
     * @return 锁键
     */
    private String buildOperationLockKey(String project, String env, String service) {
        return project + "#" + env + "#" + service;
    }

    /**
     * 尝试获取实例运维锁。
     * <p>
     * 使用短超时避免线程长时间阻塞。
     * </p>
     *
     * @param operationLock 实例锁
     * @return true 表示获取成功
     */
    private boolean tryAcquireOperationLock(ReentrantLock operationLock) {
        try {
            return operationLock.tryLock(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 根据动作与输出生成简要结果文案。
     *
     * @param action 动作
     * @param output 输出
     * @return 简要文案
     */
    private String summarizeMessage(String action, String output) {
        String normalizedAction = action == null ? "" : action.trim();
        String outputText = output == null ? "" : output.trim();
        if (outputText.isEmpty()) {
            return "操作完成: " + normalizedAction;
        }
        String firstLine = outputText.split("\\r?\\n")[0];
        if (firstLine.length() > 72) {
            firstLine = firstLine.substring(0, 72) + "...";
        }
        return "操作完成: " + normalizedAction + " | " + firstLine;
    }

    /**
     * 生成可用于日志输出的文本摘要。
     * <p>
     * 为避免日志污染，仅保留单行并限制长度。
     * </p>
     *
     * @param raw 原始文本
     * @return 摘要文本
     */
    private String summarizeOutputForLog(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        String singleLine = text.replace('\r', ' ').replace('\n', ' ');
        if (singleLine.length() <= RESULT_MAX_LOG_LEN) {
            return singleLine;
        }
        return singleLine.substring(0, RESULT_MAX_LOG_LEN) + "...";
    }

    /**
     * 提取目标实例主机标识。
     *
     * @param target 目标实例
     * @return 主机标识（无法获取时返回 unknown）
     */
    private String resolveTargetHost(LogTarget target) {
        if (target == null || target.getServerNode() == null) {
            return IpUtil.UNKNOWN;
        }
        String host = target.getServerNode().getHost();
        if (host == null || host.trim().isEmpty()) {
            return IpUtil.UNKNOWN;
        }
        return host.trim();
    }
}
