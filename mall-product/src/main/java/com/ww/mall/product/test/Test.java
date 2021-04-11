package com.ww.mall.product.test;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author:         ww
 * Datetime:       2021\3\18 0018
 * Description:
 */
@Slf4j
public class Test {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        CompletableFuture.runAsync(() -> {
//            log.info("异步执行");
//        },executor);
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            int res = 10;
            res++;
            int i = 1 / 0;
            return res;
        }, executor).whenComplete((res,exception) -> {
            log.info("whenComplete结果："+res+"======异常："+exception);
        })/*.exceptionally(exception -> {
            log.info("exceptionally异常："+exception);
            return 99;
        })*/.handleAsync((res, exception) -> {
            if(exception == null) {
                return res * 2;
            }else {
                return 0;
            }
        },executor);
        log.info("main执行完成，结果："+future.get());
    }
}
