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
        try {
            LogTarget target = logPanelQueryService.resolveExactTarget(env, service);
            String output = sshLogService.operateInstance(target, action);
            return InstanceOperationResponse.success(env, service, action, summarizeMessage(action, output), output);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "实例运维失败" : ex.getMessage();
            return InstanceOperationResponse.failure(env, service, action, message, message);
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
