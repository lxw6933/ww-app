package com.ww.app.test;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author ww
 * @create 2024-11-14- 15:05
 * @description:
 */
@Slf4j
public class Demo {

    private static final int BATCH_NUM = 1000;

    public static void main(String[] args) {
       String str = "ewqewqewqdsa";
        String s = hideExceptLastThree(str);
        System.out.println(s);
//        System.out.println(StringUtils.leftPad(String.valueOf(9999), 3, "0"));
//        async();
//        int circle = (1001 + BATCH_NUM - 1) / BATCH_NUM;
//        System.out.println(circle);
        CompletableFuture<Void> exception = CompletableFuture.runAsync(() -> {
            log.info("---------------ex");
            throw new RuntimeException("exception");
        });
        CompletableFuture.allOf(exception);
        log.info("------end---------");
    }

    private static Object async() throws ExecutionException, InterruptedException {
        CompletableFuture<Object> task = CompletableFuture.supplyAsync(Object::new).exceptionally(e -> null);
        return task.get();
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
