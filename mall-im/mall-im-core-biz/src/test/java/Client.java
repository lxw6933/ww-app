import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ww
 * @create 2024-12-23 21:03
 * @description:
 */
public class Client {

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger connectCount = new AtomicInteger(0);
        //连接bio server
        CountDownLatch count = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    Socket socket = new Socket();
                    count.await();
                    socket.connect(new InetSocketAddress(8765));
                    System.out.println("连接完成" + connectCount.getAndIncrement());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        count.countDown();
        Thread.sleep(100000);
    }

}
