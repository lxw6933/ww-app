package com.ww.app.ssh.service.support;

import com.ww.app.ssh.model.LogStreamRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
     * 2. 多个“排除”规则为 OR 关系（命中任一即过滤）。
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
        List<String> includeKeywords = new ArrayList<>();
        List<String> excludeKeywords = new ArrayList<>();
        for (LogStreamRequest.FilterRule rule : safeRules) {
            if (rule == null) {
                continue;
            }
            String type = rule.getType();
            String data = rule.getData();
            if (LogStreamRequest.FILTER_TYPE_INCLUDE.equals(type)) {
                includeKeywords.add(data);
            } else if (LogStreamRequest.FILTER_TYPE_EXCLUDE.equals(type)) {
                excludeKeywords.add(data);
            }
        }
        for (String includeKeyword : includeKeywords) {
            if (!line.contains(includeKeyword)) {
                return false;
            }
        }
        for (String excludeKeyword : excludeKeywords) {
            if (line.contains(excludeKeyword)) {
                return false;
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
        if (!includeKeyword.isEmpty() && !line.contains(includeKeyword)) {
            return false;
        }
        return excludeKeyword.isEmpty() || !line.contains(excludeKeyword);
    }
}
