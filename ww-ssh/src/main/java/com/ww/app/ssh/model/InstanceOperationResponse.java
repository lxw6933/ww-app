package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 实例运维操作响应。
 * <p>
 * 用于回传实例启停操作的执行结果与输出摘要。
 * </p>
 */
@Data
public class InstanceOperationResponse {

    /**
     * 是否执行成功。
     */
    private boolean success;

    /**
     * 目标环境。
     */
    private String env;

    /**
     * 目标服务。
     */
    private String service;

    /**
     * 执行动作。
     */
    private String action;

    /**
     * 结果说明。
     */
    private String message;

    /**
     * 命令输出（可能为空）。
     */
    private String output;

    /**
     * 执行时间戳（毫秒）。
     */
    private long executedAt;

    /**
     * 构建成功响应。
     *
     * @param env     环境
     * @param service 服务
     * @param action  动作
     * @param message 说明
     * @param output  输出
     * @return 成功响应
     */
    public static InstanceOperationResponse success(String env,
                                                    String service,
                                                    String action,
                                                    String message,
                                                    String output) {
        InstanceOperationResponse response = new InstanceOperationResponse();
        response.setSuccess(true);
        response.setEnv(env);
        response.setService(service);
        response.setAction(action);
        response.setMessage(message);
        response.setOutput(output);
        response.setExecutedAt(System.currentTimeMillis());
        return response;
    }

    /**
     * 构建失败响应。
     *
     * @param env     环境
     * @param service 服务
     * @param action  动作
     * @param message 失败原因
     * @return 失败响应
     */
    public static InstanceOperationResponse failure(String env,
                                                    String service,
                                                    String action,
                                                    String message) {
        return failure(env, service, action, message, "");
    }

    /**
     * 构建失败响应（附带输出内容）。
     *
     * @param env     环境
     * @param service 服务
     * @param action  动作
     * @param message 失败原因
     * @param output  失败输出
     * @return 失败响应
     */
    public static InstanceOperationResponse failure(String env,
                                                    String service,
                                                    String action,
                                                    String message,
                                                    String output) {
        InstanceOperationResponse response = new InstanceOperationResponse();
        response.setSuccess(false);
        response.setEnv(env);
        response.setService(service);
        response.setAction(action);
        response.setMessage(message);
        response.setOutput(output == null ? "" : output);
        response.setExecutedAt(System.currentTimeMillis());
        return response;
    }
}
