package com.ww.mall.common.utils;

/**
 * @author ww
 * @create 2024-09-19- 09:39
 * @description:
 */
public class SpecialCharacterUtil {

    private SpecialCharacterUtil() {}

    public static String escapeSpecialCharacters(String keyword) {
        // 特殊字符转义
        return keyword.replace("\\", "\\\\")
                .replace("$", "\\$")
                .replace("^", "\\^")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("|", "\\|");
    }

}
