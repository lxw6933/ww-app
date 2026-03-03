package com.ww.app.ssh.service.support;

import org.springframework.stereotype.Component;

/**
 * 日志行过滤匹配器。
 * <p>
 * 负责根据“包含/排除”规则判断单行日志是否应展示。
 * </p>
 */
@Component
public class LogLineFilterMatcher {

    /**
     * 判断当前日志行是否匹配过滤条件。
     *
     * @param line           日志行
     * @param includeKeyword 包含关键字（为空表示不启用）
     * @param excludeKeyword 排除关键字（为空表示不启用）
     * @return true 表示该日志行应展示
     */
    public boolean matches(String line, String includeKeyword, String excludeKeyword) {
        if (!includeKeyword.isEmpty() && !line.contains(includeKeyword)) {
            return false;
        }
        return excludeKeyword.isEmpty() || !line.contains(excludeKeyword);
    }
}
