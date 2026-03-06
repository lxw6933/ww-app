package com.ww.app.ssh.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 日志流订阅请求。
 * <p>
 * 由前端通过 WebSocket 发送，描述订阅范围、日志路径以及过滤条件。
 * </p>
 */
@Data
public class LogStreamRequest {

    /**
     * 全部占位值。
     */
    public static final String ALL = "__ALL__";

    /**
     * 包含规则类型。
     */
    public static final String FILTER_TYPE_INCLUDE = "include";

    /**
     * 排除规则类型。
     */
    public static final String FILTER_TYPE_EXCLUDE = "exclude";

    /**
     * 读取模式：实时 tail。
     */
    public static final String READ_MODE_TAIL = "tail";

    /**
     * 读取模式：一次性 cat。
     */
    public static final String READ_MODE_CAT = "cat";

    /**
     * 目标项目名，支持 {@link #ALL}。
     */
    private String project;

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
     * 读取模式，支持 tail/cat，默认 tail。
     */
    private String readMode = READ_MODE_TAIL;

    /**
     * 包含关键字（可选，固定字符串匹配）。
     */
    private String includeKeyword;

    /**
     * 排除关键字（可选，固定字符串匹配）。
     */
    private String excludeKeyword;

    /**
     * 链式过滤规则。
     * <p>
     * 新版前端会按“类型 + 数据”提交多条规则；
     * 若为空则回退到 includeKeyword/excludeKeyword 兼容逻辑。
     * </p>
     */
    private List<FilterRule> filterRules;

    /**
     * 判断是否选择了全部项目。
     *
     * @return true 表示全部项目
     */
    public boolean isAllProject() {
        return ALL.equalsIgnoreCase(trimToEmpty(project));
    }

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
     * 获取规范化后的读取模式。
     *
     * @return 读取模式（tail/cat）
     */
    public String normalizedReadMode() {
        String normalized = trimToEmpty(readMode).toLowerCase();
        if (READ_MODE_CAT.equals(normalized)) {
            return READ_MODE_CAT;
        }
        return READ_MODE_TAIL;
    }

    /**
     * 判断当前是否为 cat 读取模式。
     *
     * @return true 表示 cat
     */
    public boolean isCatMode() {
        return READ_MODE_CAT.equals(normalizedReadMode());
    }

    /**
     * 获取去空格后的项目名。
     *
     * @return 规范化项目名
     */
    public String normalizedProject() {
        return trimToEmpty(project);
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
     * 获取规范化后的过滤规则集合。
     *
     * @return 合法规则列表
     */
    public List<FilterRule> normalizedFilterRules() {
        if (filterRules == null || filterRules.isEmpty()) {
            return Collections.emptyList();
        }
        List<FilterRule> normalized = new ArrayList<>();
        for (FilterRule rule : filterRules) {
            if (rule == null) {
                continue;
            }
            String type = normalizeFilterType(rule.getType());
            String data = trimToEmpty(rule.getData());
            if (type.isEmpty() || data.isEmpty()) {
                continue;
            }
            FilterRule normalizedRule = new FilterRule();
            normalizedRule.setType(type);
            normalizedRule.setData(data);
            normalized.add(normalizedRule);
        }
        return normalized;
    }

    /**
     * 归一化过滤类型，兼容中英文输入。
     *
     * @param type 原始类型
     * @return 归一化后的类型（include/exclude）
     */
    private String normalizeFilterType(String type) {
        String normalized = trimToEmpty(type).toLowerCase();
        if (FILTER_TYPE_INCLUDE.equals(normalized) || "包含".equals(normalized)) {
            return FILTER_TYPE_INCLUDE;
        }
        if (FILTER_TYPE_EXCLUDE.equals(normalized) || "排除".equals(normalized)) {
            return FILTER_TYPE_EXCLUDE;
        }
        return "";
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

    /**
     * 单条过滤规则。
     */
    @Data
    public static class FilterRule {

        /**
         * 规则类型：include/exclude。
         */
        private String type;

        /**
         * 规则数据（关键字）。
         */
        private String data;
    }
}
