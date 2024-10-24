package com.ww.mall.threadLocal;

/**
 * @author ww
 * @create 2024-10-24- 11:28
 * @description:
 */
public class InheritableThreadLocalTest {

    public final static InheritableThreadLocal<Object> t1 = new InheritableThreadLocal<>();

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
                    t1.set("此值时main线程放入的！");
                }
                System.out.println("    在Main线程中取值=" + t1.get());
                Thread.sleep(100);
            }
            ThreadA a = new ThreadA();
            a.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
