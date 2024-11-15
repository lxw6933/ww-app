package com.ww.mall.test;

import cn.hutool.core.util.StrUtil;

/**
 * @author ww
 * @create 2024-11-14- 15:05
 * @description:
 */
public class Demo {

    public static void main(String[] args) {
       String str = "ewqewqewqdsa";
        String s = hideExceptLastThree(str);
        System.out.println(s);
    }

    private static String hideExceptLastThree(String str) {
        if (str == null || str.length() <= 3) {
            // 如果字符串为空或者长度小于等于3，直接返回原字符串
            return str;
        }
        // 前面的字符全部用 ***
        return "***" + StrUtil.subSuf(str, str.length() - 3);
    }

}
