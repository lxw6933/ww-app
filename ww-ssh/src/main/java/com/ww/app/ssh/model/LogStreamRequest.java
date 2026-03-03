package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 日志流订阅请求。
 * <p>
 * 由前端通过 WebSocket 发送，描述订阅范围、日志路径以及过滤条件。
 * </p>
 */
@Data
public class LogStreamRequest {

    /**
     * 全部环境占位值。
     */
    public static final String ALL = "__ALL__";

    /**
     * 目标环境名，支持 {@link #ALL}。
     */
    private String env;

    /**
     * 目标服务名，支持 {@link #ALL}。
     */
    private String service;

    /**
     * 指定日志文件路径（可选）。
     * <p>
     * 为空时使用服务默认日志目录自动发现最新日志文件。
     * </p>
     */
    private String filePath;

    /**
     * 回看行数，默认 200，范围 [10, 5000]。
     */
    private Integer lines = 200;

    /**
     * 包含关键字（可选，固定字符串匹配）。
     */
    private String includeKeyword;

    /**
     * 排除关键字（可选，固定字符串匹配）。
     */
    private String excludeKeyword;

    /**
     * 判断是否选择了全部环境。
     *
     * @return true 表示全部环境
     */
    public boolean isAllEnv() {
        return ALL.equalsIgnoreCase(trimToEmpty(env));
    }

    /**
     * 判断是否选择了全部服务。
     *
     * @return true 表示全部服务
     */
    public boolean isAllService() {
        return ALL.equalsIgnoreCase(trimToEmpty(service));
    }

    /**
     * 获取规范化后的回看行数。
     *
     * @return 限制在 [10, 5000] 范围内的行数
     */
    public int normalizedLines() {
        int resolved = lines == null ? 200 : lines;
        if (resolved < 10) {
            return 10;
        }
        return Math.min(resolved, 5000);
    }

    /**
     * 获取去空格后的环境名。
     *
     * @return 规范化环境名
     */
    public String normalizedEnv() {
        return trimToEmpty(env);
    }

    /**
     * 获取去空格后的服务名。
     *
     * @return 规范化服务名
     */
    public String normalizedService() {
        return trimToEmpty(service);
    }

    /**
     * 获取去空格后的日志路径。
     *
     * @return 规范化日志路径
     */
    public String normalizedFilePath() {
        return trimToEmpty(filePath);
    }

    /**
     * 获取去空格后的包含关键字。
     *
     * @return 规范化包含关键字
     */
    public String normalizedIncludeKeyword() {
        return trimToEmpty(includeKeyword);
    }

    /**
     * 获取去空格后的排除关键字。
     *
     * @return 规范化排除关键字
     */
    public String normalizedExcludeKeyword() {
        return trimToEmpty(excludeKeyword);
    }

    /**
     * 对字符串进行去空格并处理 null。
     *
     * @param source 原字符串
     * @return 非 null 字符串
     */
    private String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
    }
}
