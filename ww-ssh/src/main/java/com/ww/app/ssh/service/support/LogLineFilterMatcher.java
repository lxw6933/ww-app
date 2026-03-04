package com.ww.app.ssh.service.support;

import com.ww.app.ssh.model.LogStreamRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 日志行过滤匹配器。
 * <p>
 * 负责根据“包含/排除”规则判断单行日志是否应展示。
 * </p>
 */
@Component
public class LogLineFilterMatcher {

    /**
     * 按链式规则进行匹配。
     * <p>
     * 规则策略：
     * 1. 多个“包含”规则为 AND 关系（全部命中才保留）；<br>
     * 2. 多个“排除”规则为 OR 关系（命中任一即过滤）；<br>
     * 3. 单条规则内部支持 {@code &&}/{@code ||} 表达式（先与后或）。
     * </p>
     *
     * @param line        日志行
     * @param filterRules 过滤规则集合
     * @return true 表示该日志行应展示
     */
    public boolean matches(String line, List<LogStreamRequest.FilterRule> filterRules) {
        if (line == null) {
            return false;
        }
        List<LogStreamRequest.FilterRule> safeRules =
                filterRules == null ? Collections.emptyList() : filterRules;
        if (safeRules.isEmpty()) {
            return true;
        }
        for (LogStreamRequest.FilterRule rule : safeRules) {
            if (rule == null) {
                continue;
            }
            String type = rule.getType();
            String data = rule.getData();
            if (LogStreamRequest.FILTER_TYPE_INCLUDE.equals(type)) {
                if (!matchesKeywordExpression(line, data)) {
                    return false;
                }
            } else if (LogStreamRequest.FILTER_TYPE_EXCLUDE.equals(type)) {
                if (matchesKeywordExpression(line, data)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断当前日志行是否匹配过滤条件。
     *
     * @param line           日志行
     * @param includeKeyword 包含关键字（为空表示不启用）
     * @param excludeKeyword 排除关键字（为空表示不启用）
     * @return true 表示该日志行应展示
     */
    public boolean matches(String line, String includeKeyword, String excludeKeyword) {
        if (line == null) {
            return false;
        }
        if (!includeKeyword.isEmpty() && !matchesKeywordExpression(line, includeKeyword)) {
            return false;
        }
        return excludeKeyword.isEmpty() || !matchesKeywordExpression(line, excludeKeyword);
    }

    /**
     * 判断日志行是否命中关键字表达式。
     * <p>
     * 支持两种逻辑运算符：
     * 1. {@code &&}：同组内全部词都出现才命中；<br>
     * 2. {@code ||}：任一分组命中即命中。<br>
     * 运算优先级为 {@code &&} 高于 {@code ||}，即按“先与后或”解析。
     * </p>
     *
     * @param line       日志行
     * @param expression 关键字表达式
     * @return true 表示命中
     */
    private boolean matchesKeywordExpression(String line, String expression) {
        String normalizedExpression = expression == null ? "" : expression.trim();
        if (normalizedExpression.isEmpty()) {
            return false;
        }
        String[] orGroups = normalizedExpression.split("\\|\\|");
        for (String group : orGroups) {
            if (matchesAndGroup(line, group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算单个 AND 分组是否命中。
     *
     * @param line  日志行
     * @param group AND 分组
     * @return true 表示该分组命中
     */
    private boolean matchesAndGroup(String line, String group) {
        String[] terms = (group == null ? "" : group).split("&&");
        boolean hasValidTerm = false;
        for (String termRaw : terms) {
            String term = termRaw == null ? "" : termRaw.trim();
            if (term.isEmpty()) {
                continue;
            }
            hasValidTerm = true;
            if (!line.contains(term)) {
                return false;
            }
        }
        return hasValidTerm;
    }
}
