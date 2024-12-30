package com.ww.app.cpu;

import sun.misc.Contended;

/**
 * @author ww
 * @create 2024-08-30 22:44
 * @description:
 */
public class FalseShareTest {

    public static void main(String[] args) throws InterruptedException {
        Share share = new Share();
        long start = System.currentTimeMillis();
        Thread a = new Thread(() -> {
            for (int i = 0; i < 100000000; i++) {
                share.x ++;
            }
        });
        Thread b = new Thread(() -> {
            for (int i = 0; i < 100000000; i++) {
                share.y ++;
            }
        });

        a.start();
        b.start();
        a.join();
        b.join();
        long end = System.currentTimeMillis();
        System.out.println(share.x + "==" + share.y);
        System.out.println(end - start);
    }

    static class Share {
        @Contended
        private volatile long x;
        @Contended
        private volatile long y;
    }

}
