package com.ww.app.im;

import com.alibaba.fastjson.JSON;
import com.ww.app.im.api.dto.MessageDTO;
import com.ww.app.im.api.enums.ImMsgBizCodeEnum;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.component.ImHandlerComponent;
import com.ww.app.im.component.ImMsgSerializerComponent;
import com.ww.app.im.core.api.common.ImConstant;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.pool.ImMsgEventPool;
import com.ww.app.im.utils.ImChannelHandlerContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 多用户在线单聊模拟测试
 * 
 * 测试场景：
 * 1. 模拟多个用户上线（建立连接）
 * 2. 模拟用户之间发送单聊消息
 * 3. 模拟用户下线（断开连接）
 * 4. 并发上线/下线场景
 * 5. 并发发送消息场景
 * 6. 消息送达率统计
 * 7. 性能压力测试
 * 
 * @author ww
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiUserChatSimulationTest {
    
    @Resource
    private ImHandlerComponent imHandlerComponent;
    
    @Resource
    private ImMsgSerializerComponent imMsgSerializerComponent;
    
    @Resource
    private ImMsgEventPool imMsgEventPool;
    
    /**
     * 模拟用户连接池
     */
    private final Map<Long, MockUserConnection> userConnections = new ConcurrentHashMap<>();
    
    /**
     * 消息统计
     */
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicInteger onlineUsers = new AtomicInteger(0);
    
    /**
     * 线程池
     */
    private ExecutorService executorService;
    
    @BeforeEach
    public void setUp() {
        log.info("========================================");
        log.info("初始化测试环境...");
        executorService = Executors.newFixedThreadPool(20);
        userConnections.clear();
        totalMessagesSent.set(0);
        totalMessagesReceived.set(0);
        onlineUsers.set(0);
        log.info("测试环境初始化完成");
        log.info("========================================\n");
    }
    
    @AfterEach
    public void tearDown() {
        log.info("\n========================================");
        log.info("清理测试环境...");
        
        // 下线所有用户
        userConnections.values().forEach(MockUserConnection::disconnect);
        userConnections.clear();
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        log.info("测试环境清理完成");
        log.info("========================================");
    }
    
    @Test
    @Order(1)
    @DisplayName("测试1: 单个用户上线下线")
    public void test01_SingleUserOnlineOffline() throws Exception {
        log.info("\n【测试1】单个用户上线下线");
        log.info("----------------------------");
        
        // 用户上线
        Long userId = 10001L;
        MockUserConnection user = createAndConnectUser(userId, "Alice");
        
        assertTrue(user.isConnected(), "用户应该在线");
        assertEquals(1, onlineUsers.get(), "在线用户数应为1");
        
        // 用户下线
        user.disconnect();
        
        assertFalse(user.isConnected(), "用户应该离线");
        assertEquals(0, onlineUsers.get(), "在线用户数应为0");
        
        log.info("✅ 测试1通过：单用户上线下线正常\n");
    }
    
    @Test
    @Order(2)
    @DisplayName("测试2: 多用户顺序上线下线")
    public void test02_MultiUserSequentialOnlineOffline() throws Exception {
        log.info("\n【测试2】多用户顺序上线下线");
        log.info("----------------------------");
        
        int userCount = 10;
        List<MockUserConnection> users = new ArrayList<>();
        
        // 用户依次上线
        log.info(">>> 用户上线阶段");
        for (int i = 0; i < userCount; i++) {
            Long userId = 10001L + i;
            String username = "User" + (i + 1);
            MockUserConnection user = createAndConnectUser(userId, username);
            users.add(user);
            
            log.info("  [{}] {} 上线成功", userId, username);
            Thread.sleep(100); // 模拟上线间隔
        }
        
        assertEquals(userCount, onlineUsers.get(), 
                String.format("在线用户数应为%d", userCount));
        log.info(">>> 当前在线: {} 人\n", onlineUsers.get());
        
        // 用户依次下线
        log.info(">>> 用户下线阶段");
        for (MockUserConnection user : users) {
            user.disconnect();
            log.info("  [{}] {} 下线", user.getUserId(), user.getUsername());
            Thread.sleep(100);
        }
        
        assertEquals(0, onlineUsers.get(), "所有用户应已下线");
        log.info(">>> 当前在线: {} 人", onlineUsers.get());
        
        log.info("✅ 测试2通过：多用户顺序上线下线正常\n");
    }
    
    @Test
    @Order(3)
    @DisplayName("测试3: 两个用户单聊通信")
    public void test03_TwoUsersSingleChat() throws Exception {
        log.info("\n【测试3】两个用户单聊通信");
        log.info("----------------------------");
        
        // 创建两个用户
        MockUserConnection alice = createAndConnectUser(10001L, "Alice");
        MockUserConnection bob = createAndConnectUser(10002L, "Bob");
        
        log.info(">>> Alice 和 Bob 已上线\n");
        
        // Alice 给 Bob 发送消息
        String message1 = "Hi Bob, how are you?";
        alice.sendMessage(bob.getUserId(), message1);
        totalMessagesSent.incrementAndGet();
        
        log.info("  [Alice -> Bob]: {}", message1);
        Thread.sleep(500);
        
        // Bob 给 Alice 发送消息
        String message2 = "Hi Alice, I'm fine, thank you!";
        bob.sendMessage(alice.getUserId(), message2);
        totalMessagesSent.incrementAndGet();
        
        log.info("  [Bob -> Alice]: {}", message2);
        Thread.sleep(500);
        
        // 继续对话
        alice.sendMessage(bob.getUserId(), "Great! Let's have lunch together.");
        bob.sendMessage(alice.getUserId(), "Sure! See you at noon.");
        totalMessagesSent.addAndGet(2);
        
        log.info("  [Alice -> Bob]: Great! Let's have lunch together.");
        log.info("  [Bob -> Alice]: Sure! See you at noon.");
        
        Thread.sleep(1000);
        
        log.info("\n>>> 聊天统计:");
        log.info("  总发送: {} 条", totalMessagesSent.get());
        assertTrue(totalMessagesSent.get() >= 4, "应该发送了至少4条消息");
        
        log.info("✅ 测试3通过：两用户单聊通信正常\n");
    }
    
    @Test
    @Order(4)
    @DisplayName("测试4: 多用户交叉单聊")
    public void test04_MultiUserCrossChat() throws Exception {
        log.info("\n【测试4】多用户交叉单聊");
        log.info("----------------------------");
        
        // 创建5个用户
        MockUserConnection user1 = createAndConnectUser(10001L, "Alice");
        MockUserConnection user2 = createAndConnectUser(10002L, "Bob");
        MockUserConnection user3 = createAndConnectUser(10003L, "Charlie");
        MockUserConnection user4 = createAndConnectUser(10004L, "David");
        MockUserConnection user5 = createAndConnectUser(10005L, "Eve");
        
        log.info(">>> 5个用户已上线\n");
        
        // 模拟交叉聊天
        log.info(">>> 开始交叉聊天:");
        
        user1.sendMessage(user2.getUserId(), "Alice -> Bob: Hello!");
        user2.sendMessage(user1.getUserId(), "Bob -> Alice: Hi there!");
        user3.sendMessage(user4.getUserId(), "Charlie -> David: How's it going?");
        user4.sendMessage(user3.getUserId(), "David -> Charlie: All good!");
        user5.sendMessage(user1.getUserId(), "Eve -> Alice: Long time no see!");
        user1.sendMessage(user5.getUserId(), "Alice -> Eve: Indeed! How have you been?");
        user2.sendMessage(user3.getUserId(), "Bob -> Charlie: Hey Charlie!");
        user3.sendMessage(user2.getUserId(), "Charlie -> Bob: Hey Bob!");
        user4.sendMessage(user5.getUserId(), "David -> Eve: Nice to meet you!");
        user5.sendMessage(user4.getUserId(), "Eve -> David: Nice to meet you too!");
        
        int expectedMessages = 10;
        totalMessagesSent.addAndGet(expectedMessages);
        
        Thread.sleep(2000);
        
        log.info("\n>>> 聊天统计:");
        log.info("  在线用户: {} 人", onlineUsers.get());
        log.info("  总发送: {} 条", totalMessagesSent.get());
        
        assertEquals(5, onlineUsers.get(), "应有5个用户在线");
        assertTrue(totalMessagesSent.get() >= expectedMessages, 
                String.format("应该发送了至少%d条消息", expectedMessages));
        
        log.info("✅ 测试4通过：多用户交叉单聊正常\n");
    }
    
    @Test
    @Order(5)
    @DisplayName("测试5: 并发用户上线")
    public void test05_ConcurrentUserOnline() throws Exception {
        log.info("\n【测试5】并发用户上线");
        log.info("----------------------------");
        
        int userCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        log.info(">>> 准备 {} 个用户并发上线...", userCount);
        
        // 创建并发上线任务
        for (int i = 0; i < userCount; i++) {
            final Long userId = 20001L + i;
            final String username = "ConcurrentUser" + (i + 1);
            
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 等待统一开始
                    
                    MockUserConnection user = createAndConnectUser(userId, username);
                    if (user.isConnected()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    log.error("用户{}上线失败", userId, e);
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 开始计时
        long startTime = System.currentTimeMillis();
        startLatch.countDown(); // 统一开始
        
        // 等待所有用户上线完成
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(completed, "所有用户应在30秒内完成上线");
        
        log.info("\n>>> 并发上线结果:");
        log.info("  目标用户数: {}", userCount);
        log.info("  成功上线: {}", successCount.get());
        log.info("  失败数量: {}", failCount.get());
        log.info("  当前在线: {}", onlineUsers.get());
        log.info("  总耗时: {} ms", duration);
        log.info("  平均耗时: {} ms/用户", duration / userCount);
        
        assertTrue(successCount.get() >= userCount * 0.95, 
                "至少95%的用户应成功上线");
        
        log.info("✅ 测试5通过：并发用户上线正常\n");
    }
    
    @Test
    @Order(6)
    @DisplayName("测试6: 并发发送消息")
    public void test06_ConcurrentSendMessages() throws Exception {
        log.info("\n【测试6】并发发送消息");
        log.info("----------------------------");
        
        // 先创建10个用户
        int userCount = 10;
        List<MockUserConnection> users = new ArrayList<>();
        
        log.info(">>> 创建 {} 个用户...", userCount);
        for (int i = 0; i < userCount; i++) {
            MockUserConnection user = createAndConnectUser(30001L + i, "User" + (i + 1));
            users.add(user);
        }
        
        Thread.sleep(1000);
        log.info(">>> {} 个用户已上线\n", onlineUsers.get());
        
        // 每个用户并发发送消息
        int messagesPerUser = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(userCount);
        AtomicInteger totalSent = new AtomicInteger(0);
        
        log.info(">>> 开始并发发送消息 (每用户{}条)...", messagesPerUser);
        
        for (MockUserConnection sender : users) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    Random random = new Random();
                    for (int i = 0; i < messagesPerUser; i++) {
                        // 随机选择一个接收者
                        MockUserConnection receiver = users.get(random.nextInt(users.size()));
                        if (!receiver.getUserId().equals(sender.getUserId())) {
                            String content = String.format("[%s->%s] Message %d", 
                                    sender.getUsername(), receiver.getUsername(), i + 1);
                            sender.sendMessage(receiver.getUserId(), content);
                            totalSent.incrementAndGet();
                        }
                        
                        // 随机延迟，模拟真实聊天
                        Thread.sleep(random.nextInt(50));
                    }
                    
                } catch (Exception e) {
                    log.error("发送消息失败", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 开始计时
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // 等待完成
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(completed, "所有消息应在60秒内发送完成");
        
        Thread.sleep(2000); // 等待消息处理
        
        int expectedTotal = userCount * messagesPerUser;
        double tps = (double) totalSent.get() / duration * 1000;
        
        log.info("\n>>> 并发发送结果:");
        log.info("  在线用户: {}", onlineUsers.get());
        log.info("  期望发送: {} 条", expectedTotal);
        log.info("  实际发送: {} 条", totalSent.get());
        log.info("  发送成功率: {:.2f}%", (double) totalSent.get() / expectedTotal * 100);
        log.info("  总耗时: {} ms", duration);
        log.info("  TPS: {:.2f}", tps);
        log.info("  平均延迟: {:.2f} ms", (double) duration / totalSent.get());
        
        assertTrue(totalSent.get() >= expectedTotal * 0.9, 
                "至少90%的消息应成功发送");
        
        log.info("✅ 测试6通过：并发发送消息正常\n");
    }
    
    @Test
    @Order(7)
    @DisplayName("测试7: 并发上线下线场景")
    public void test07_ConcurrentOnlineOffline() throws Exception {
        log.info("\n【测试7】并发上线下线场景");
        log.info("----------------------------");
        
        int userCount = 30;
        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger maxOnline = new AtomicInteger(0);
        
        log.info(">>> 模拟 {} 个用户随机上线下线...", userCount);
        
        for (int i = 0; i < userCount; i++) {
            final Long userId = 40001L + i;
            final String username = "FluctuateUser" + (i + 1);
            
            executorService.submit(() -> {
                try {
                    Random random = new Random();
                    
                    // 随机延迟后上线
                    Thread.sleep(random.nextInt(2000));
                    MockUserConnection user = createAndConnectUser(userId, username);
                    int current = onlineUsers.get();
                    maxOnline.updateAndGet(max -> Math.max(max, current));
                    log.info("  [+] {} 上线 (当前在线: {})", username, current);
                    
                    // 在线一段时间
                    Thread.sleep(random.nextInt(3000) + 1000);
                    
                    // 下线
                    user.disconnect();
                    log.info("  [-] {} 下线 (当前在线: {})", username, onlineUsers.get());
                    
                } catch (Exception e) {
                    log.error("用户{}操作失败", userId, e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有用户完成
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "所有用户应在60秒内完成");
        
        log.info("\n>>> 上线下线结果:");
        log.info("  总用户数: {}", userCount);
        log.info("  最高在线: {}", maxOnline.get());
        log.info("  当前在线: {}", onlineUsers.get());
        
        assertEquals(0, onlineUsers.get(), "最终应该没有用户在线");
        assertTrue(maxOnline.get() > 0, "应该有用户曾经在线");
        
        log.info("✅ 测试7通过：并发上线下线正常\n");
    }
    
    @Test
    @Order(8)
    @DisplayName("测试8: 压力测试 - 100用户1000消息")
    public void test08_StressTest() throws Exception {
        log.info("\n【测试8】压力测试");
        log.info("----------------------------");
        
        int userCount = 100;
        int totalMessages = 1000;
        
        log.info(">>> 创建 {} 个用户...", userCount);
        List<MockUserConnection> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            MockUserConnection user = createAndConnectUser(50001L + i, "StressUser" + (i + 1));
            users.add(user);
        }
        
        Thread.sleep(2000);
        log.info(">>> {} 个用户已上线\n", onlineUsers.get());
        
        log.info(">>> 开始发送 {} 条消息...", totalMessages);
        
        CountDownLatch sendLatch = new CountDownLatch(totalMessages);
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 并发发送消息
        for (int i = 0; i < totalMessages; i++) {
            executorService.submit(() -> {
                try {
                    Random random = new Random();
                    MockUserConnection sender = users.get(random.nextInt(users.size()));
                    MockUserConnection receiver = users.get(random.nextInt(users.size()));
                    
                    if (!sender.getUserId().equals(receiver.getUserId())) {
                        String content = "Stress test message from " + sender.getUsername();
                        sender.sendMessage(receiver.getUserId(), content);
                        sentCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    sendLatch.countDown();
                }
            });
        }
        
        // 等待发送完成
        boolean completed = sendLatch.await(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(completed, "所有消息应在120秒内发送完成");
        
        Thread.sleep(3000); // 等待处理
        
        double tps = (double) sentCount.get() / duration * 1000;
        double successRate = (double) sentCount.get() / totalMessages * 100;
        
        log.info("\n>>> 压力测试结果:");
        log.info("  在线用户: {}", onlineUsers.get());
        log.info("  目标消息: {}", totalMessages);
        log.info("  成功发送: {}", sentCount.get());
        log.info("  发送失败: {}", errorCount.get());
        log.info("  成功率: {:.2f}%", successRate);
        log.info("  总耗时: {} ms", duration);
        log.info("  TPS: {:.2f}", tps);
        log.info("  平均延迟: {:.2f} ms", (double) duration / sentCount.get());
        
        assertTrue(successRate >= 95.0, "成功率应 >= 95%");
        assertTrue(tps > 50, "TPS应 > 50");
        
        log.info("✅ 测试8通过：压力测试正常\n");
    }
    
    @Test
    @Order(9)
    @DisplayName("测试9: 消息可达性测试")
    public void test09_MessageReachability() throws Exception {
        log.info("\n【测试9】消息可达性测试");
        log.info("----------------------------");
        
        // 创建发送者和接收者
        MockUserConnection sender = createAndConnectUser(60001L, "Sender");
        MockUserConnection receiver = createAndConnectUser(60002L, "Receiver");
        
        int messageCount = 50;
        AtomicInteger receivedCount = new AtomicInteger(0);
        
        // 设置接收者的消息监听
        receiver.setMessageListener(msg -> {
            receivedCount.incrementAndGet();
            log.debug("  [接收] {}", msg);
        });
        
        log.info(">>> 发送 {} 条消息...\n", messageCount);
        
        // 发送消息
        for (int i = 0; i < messageCount; i++) {
            String content = "Message " + (i + 1);
            sender.sendMessage(receiver.getUserId(), content);
            Thread.sleep(50);
        }
        
        // 等待消息送达
        Thread.sleep(3000);
        
        double reachability = (double) receivedCount.get() / messageCount * 100;
        
        log.info("\n>>> 可达性统计:");
        log.info("  发送消息: {}", messageCount);
        log.info("  接收消息: {}", receivedCount.get());
        log.info("  可达率: {:.2f}%", reachability);
        
        assertTrue(reachability >= 90.0, "消息可达率应 >= 90%");
        
        log.info("✅ 测试9通过：消息可达性正常\n");
    }
    
    @Test
    @Order(10)
    @DisplayName("测试10: 综合场景测试")
    public void test10_ComprehensiveScenario() throws Exception {
        log.info("\n【测试10】综合场景测试");
        log.info("----------------------------");
        
        log.info(">>> 模拟真实聊天场景...\n");
        
        // 第一批用户上线
        log.info(">>> 阶段1: 初始用户上线");
        List<MockUserConnection> initialUsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MockUserConnection user = createAndConnectUser(70001L + i, "InitialUser" + (i + 1));
            initialUsers.add(user);
        }
        log.info("  初始用户: {} 人\n", onlineUsers.get());
        Thread.sleep(1000);
        
        // 开始聊天
        log.info(">>> 阶段2: 开始聊天");
        for (int i = 0; i < 10; i++) {
            Random random = new Random();
            MockUserConnection sender = initialUsers.get(random.nextInt(initialUsers.size()));
            MockUserConnection receiver = initialUsers.get(random.nextInt(initialUsers.size()));
            
            if (!sender.equals(receiver)) {
                sender.sendMessage(receiver.getUserId(), "Initial chat message " + i);
                Thread.sleep(200);
            }
        }
        log.info("  初始聊天完成\n");
        
        // 新用户加入
        log.info(">>> 阶段3: 新用户加入");
        List<MockUserConnection> newUsers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MockUserConnection user = createAndConnectUser(70010L + i, "NewUser" + (i + 1));
            newUsers.add(user);
            initialUsers.add(user);
        }
        log.info("  新用户加入后: {} 人\n", onlineUsers.get());
        Thread.sleep(1000);
        
        // 继续聊天
        log.info(">>> 阶段4: 继续聊天");
        for (int i = 0; i < 20; i++) {
            Random random = new Random();
            MockUserConnection sender = initialUsers.get(random.nextInt(initialUsers.size()));
            MockUserConnection receiver = initialUsers.get(random.nextInt(initialUsers.size()));
            
            if (!sender.equals(receiver)) {
                sender.sendMessage(receiver.getUserId(), "Continued chat " + i);
                Thread.sleep(100);
            }
        }
        log.info("  持续聊天完成\n");
        
        // 部分用户下线
        log.info(">>> 阶段5: 部分用户下线");
        for (int i = 0; i < 2; i++) {
            initialUsers.get(i).disconnect();
        }
        log.info("  用户下线后: {} 人\n", onlineUsers.get());
        Thread.sleep(1000);
        
        // 剩余用户继续聊天
        log.info(">>> 阶段6: 剩余用户继续聊天");
        List<MockUserConnection> remainingUsers = initialUsers.subList(2, initialUsers.size());
        for (int i = 0; i < 15; i++) {
            Random random = new Random();
            MockUserConnection sender = remainingUsers.get(random.nextInt(remainingUsers.size()));
            MockUserConnection receiver = remainingUsers.get(random.nextInt(remainingUsers.size()));
            
            if (!sender.equals(receiver)) {
                sender.sendMessage(receiver.getUserId(), "Final chat " + i);
                Thread.sleep(150);
            }
        }
        log.info("  最后聊天完成\n");
        
        // 全部下线
        log.info(">>> 阶段7: 全部下线");
        remainingUsers.forEach(MockUserConnection::disconnect);
        
        log.info("\n>>> 综合场景统计:");
        log.info("  最终在线: {} 人", onlineUsers.get());
        log.info("  总发送消息: {} 条", totalMessagesSent.get());
        
        assertEquals(0, onlineUsers.get(), "所有用户应已下线");
        assertTrue(totalMessagesSent.get() > 0, "应该有消息被发送");
        
        log.info("✅ 测试10通过：综合场景测试正常\n");
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 创建并连接用户
     */
    private MockUserConnection createAndConnectUser(Long userId, String username) {
        MockUserConnection connection = new MockUserConnection(userId, username);
        connection.connect();
        userConnections.put(userId, connection);
        onlineUsers.incrementAndGet();
        return connection;
    }
    
    /**
     * 模拟用户连接
     */
    private class MockUserConnection {
        @Getter
        private final Long userId;
        @Getter
        private final String username;
        private ChannelHandlerContext mockCtx;
        @Getter
        private boolean connected = false;
        @Setter
        private Consumer<String> messageListener;
        
        public MockUserConnection(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
        
        public void connect() {
            // Mock Netty ChannelHandlerContext
            mockCtx = mock(ChannelHandlerContext.class);
            Channel mockChannel = mock(Channel.class);
            when(mockCtx.channel()).thenReturn(mockChannel);
            when(mockChannel.isActive()).thenReturn(true);
            
            // 注册到连接管理器
            ImChannelHandlerContextUtils.set(userId, mockCtx);
            
            connected = true;
            log.debug("用户 {} [{}] 建立连接", username, userId);
        }
        
        public void disconnect() {
            if (connected) {
                ImChannelHandlerContextUtils.remove(userId);
                connected = false;
                onlineUsers.decrementAndGet();
                userConnections.remove(userId);
                log.debug("用户 {} [{}] 断开连接", username, userId);
            }
        }
        
        public void sendMessage(Long toUserId, String content) {
            if (!connected) {
                log.warn("用户 {} 未连接，无法发送消息", username);
                return;
            }
            
            try {
                // 构造消息
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setContent(content);
                messageDTO.setUserId(toUserId);
                
                ImMsgBody imMsgBody = new ImMsgBody();
                imMsgBody.setUserId(userId);
                imMsgBody.setBizCode(ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode());
                imMsgBody.setBizMsg(JSON.toJSONString(messageDTO));
                
                // 序列化
                byte[] bodyBytes = imMsgSerializerComponent.serializeMsg(
                        ImConstant.DEFAULT_SERIALIZER, imMsgBody);
                
                // 构造ImMsg
                ImMsg imMsg = new ImMsg();
                imMsg.setMagic(ImConstant.DEFAULT_MAGIC);
                imMsg.setVersion(ImConstant.DEFAULT_VERSION);
                imMsg.setSerializeType(ImConstant.DEFAULT_SERIALIZER);
                imMsg.setMsgType(3); // 业务消息
                imMsg.setLength(bodyBytes.length);
                imMsg.setBody(bodyBytes);
                
                // 模拟处理
                imHandlerComponent.handle(mockCtx, imMsg);
                
                log.debug("[{}] -> [{}]: {}", username, toUserId, content);
                
                // 通知接收者（如果有监听器）
                MockUserConnection receiver = userConnections.get(toUserId);
                if (receiver != null && receiver.messageListener != null) {
                    receiver.messageListener.accept(content);
                }
                
            } catch (Exception e) {
                log.error("发送消息失败: {} -> {}", userId, toUserId, e);
            }
        }

    }
}
