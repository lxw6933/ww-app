package com.ww.mall.threadLocal;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ww
 * @create 2024-10-24- 11:28
 * @description:
 */
public class InheritableThreadLocalTest {

    public final static ThreadLocal<String> t1 = new ThreadLocal<>();
    public final static InheritableThreadLocal<String> t2 = new InheritableThreadLocal<>();
    public final static TransmittableThreadLocal<String> t3 = new TransmittableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
//        t1();
//        t2();
//        t22();
//        t3();
        t33();
    }

    private static void t1() throws InterruptedException {
        // 使用ThreadLocal，线程池中的子线程是无法拿到主线程设置到ThreadLocal的值的
        ExecutorService executor = Executors.newFixedThreadPool(1);
        for (int i = 0; i < 5; i++) {
            t1.set("ThreadLocal" + i);
            executor.execute(() -> {
                // 获取t1的值，是否能获取到主线程set的值
                System.out.println(Thread.currentThread().getId() + " threadLocal get: " + t1.get());
                // 子线程自定义t1的值，看主线程是否能获取到
                t1.set("ThreadLocal t1 value");
            });
        }
        // 保证线程次执行完
        Thread.sleep(1000);
        // 获取t1的值，是否能获取到子线程更新的值
        System.out.println("threadLocal main get t1: " + t1.get());
        t1.remove();
        executor.shutdown();
    }

    private static void t2() throws InterruptedException {
        // 使用InheritableThreadLocal，线程池中的子线程是可以拿到主线程设置到InheritableThreadLocal的值的
        // 但是主线程中ThreadLocal值再更改，子线程获取到的仍然是首次赋值拿到的值，故其弊端是核心线程不会每次重建，所以值也就不会更新。
        ExecutorService executor = Executors.newFixedThreadPool(1);
        for (int i = 0; i < 5; i++) {
            t2.set("InheritableThreadLocal" + i);
            executor.execute(() -> System.out.println(Thread.currentThread().getId() + " InheritableThreadLocal get: " + t2.get()));
        }
        t2.remove();
        executor.shutdown();
    }

    private static void t22() throws InterruptedException {
        // InheritableThreadLocal子线程更新t2，各个子线程之间无法感知
        ExecutorService executor = Executors.newFixedThreadPool(3);
        t2.set("InheritableThreadLocal");
        for (int i = 0; i < 9; i++) {
            executor.execute(() -> {
                System.out.println(Thread.currentThread().getId() + " InheritableThreadLocal get: " + t2.get());
                t2.set("update InheritableThreadLocal[" + Thread.currentThread().getId() + "]");
            });
        }
        t2.remove();
        executor.shutdown();
    }

    private static void t3() throws InterruptedException {
        // 解决了InheritableThreadLocal的问题
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor = TtlExecutors.getTtlExecutorService(executor);
        for (int i = 0; i < 5; i++) {
            t3.set("TransmittableThreadLocal" + i);
            executor.submit(() -> System.out.println(Thread.currentThread().getId() + " TransmittableThreadLocal get: " + t3.get()));
        }
        t3.remove();
        executor.shutdown();
    }

    private static void t33() throws InterruptedException {
        // TransmittableThreadLocal子线程更新t3，各个子线程之间无法感知
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor = TtlExecutors.getTtlExecutorService(executor);
        t3.set("TransmittableThreadLocal" + Thread.currentThread().getId());
        for (int i = 0; i < 9; i++) {
            executor.execute(() -> {
                System.out.println(Thread.currentThread().getId() + " TransmittableThreadLocal get: " + t3.get());
                t3.set("update TransmittableThreadLocal[" + Thread.currentThread().getId() + "]");
            });
        }
        Thread.sleep(1000);
        t3.set("TransmittableThreadLocal" + Thread.currentThread().getName());
        System.out.println("==================================================" + Thread.currentThread().getName());
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                System.out.println(Thread.currentThread().getId() + " TransmittableThreadLocal get: " + t3.get());
            });
        }
        t3.remove();
        executor.shutdown();
    }

}
