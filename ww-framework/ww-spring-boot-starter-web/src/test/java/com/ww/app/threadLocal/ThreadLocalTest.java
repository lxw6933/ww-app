package com.ww.app.threadLocal;

/**
 * @author ww
 * @create 2024-10-24- 11:24
 * @description:
 */
public class ThreadLocalTest {

    public final static ThreadLocal<Object> t1 = new ThreadLocal<>();

    static class ThreadA extends Thread {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10; i++) {
                    System.out.println("在ThreadA线程中取值=" + t1.get());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            for (int i = 0; i < 10; i++) {
                if (t1.get() == null) {
                    t1.set("此值是main线程放入的！");
                }
                System.out.println("    在Main线程中取值=" + t1.get());
                Thread.sleep(100);
            }
            Thread.sleep(100);
            ThreadA a = new ThreadA();
            a.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
