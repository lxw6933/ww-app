package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.InstanceOperationRequest;
import com.ww.app.ssh.model.InstanceOperationResponse;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 锁粒度为 env+service，防止同一实例并发执行多个运维动作。
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
     * @return 操作响应
     */
    @PostMapping("/operate")
    public InstanceOperationResponse operate(@RequestBody InstanceOperationRequest request) {
        String env = request == null ? "" : request.normalizedEnv();
        String service = request == null ? "" : request.normalizedService();
        String action = request == null ? "" : request.normalizedAction();
        if (env.isEmpty() || service.isEmpty() || action.isEmpty()) {
            return InstanceOperationResponse.failure(env, service, action, "参数不完整：env/service/action 必填");
        }
        String lockKey = buildOperationLockKey(env, service);
        ReentrantLock operationLock = operationLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        boolean locked = false;
        try {
            locked = tryAcquireOperationLock(operationLock);
            if (!locked) {
                return InstanceOperationResponse.failure(env, service, action, "当前实例有运维操作执行中，请稍后重试");
            }
            LogTarget target = logPanelQueryService.resolveExactTarget(env, service);
            String output = sshLogService.operateInstance(target, action);
            return InstanceOperationResponse.success(env, service, action, summarizeMessage(action, output), output);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "实例运维失败" : ex.getMessage();
            return InstanceOperationResponse.failure(env, service, action, message, message);
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
     * @param env     环境名
     * @param service 实例服务键
     * @return 锁键
     */
    private String buildOperationLockKey(String env, String service) {
        return env + "#" + service;
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
}
