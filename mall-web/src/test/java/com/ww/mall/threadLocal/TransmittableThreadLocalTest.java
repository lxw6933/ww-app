package com.ww.mall.threadLocal;

import lombok.Data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ww
 * @create 2024-11-15- 15:04
 * @description:
 */
public class TransmittableThreadLocalTest {

//    private static final TransmittableThreadLocal<Test> t = new TransmittableThreadLocal<>();
    private static final InheritableThreadLocal<Test> t = new InheritableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
//        executor = TtlExecutors.getTtlExecutorService(executor);

        Test test = new Test();
        test.setA("main a");
        test.setB(1);
        t.set(test);
        CompletableFuture.runAsync(() -> {
            System.out.println(Thread.currentThread().getId() + "线程：" + t.get());
            t.get().setA(Thread.currentThread().getId() + "线程：");
            t.get().add();
        }, executor);
        Thread.sleep(1000);
        System.out.println("主线程：" + t.get());
        t.get().add();
        t.get().add();
        t.get().add();
        t.get().add();
        CompletableFuture.runAsync(() -> {
            System.out.println(Thread.currentThread().getId() + "线程：" + t.get());
            t.get().setA(Thread.currentThread().getId() + "线程：");
            t.get().add();
        }, executor);
        Thread.sleep(1000);
        System.out.println("主线程：" + t.get());
        CompletableFuture.runAsync(() -> {
            System.out.println(Thread.currentThread().getId() + "线程：" + t.get());
            t.get().setA(Thread.currentThread().getId() + "线程：");
            t.get().add();
        }, executor);
        Thread.sleep(1000);
        System.out.println("主线程：" + t.get());
    }

    @Data
    static class Test {
        private String a;
        private Integer b;

        public void add() {
            b++;
        }
    }

}
