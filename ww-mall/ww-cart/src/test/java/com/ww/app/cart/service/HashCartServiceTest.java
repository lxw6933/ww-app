package com.ww.app.cart.service;

import com.alibaba.fastjson.JSON;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.MoneyUtils;
import com.ww.app.common.utils.ThreadUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 购物车服务测试类（增强版）
 * 测试内容：
 * 1. 基本功能测试
 * 2. 边界值测试
 * 3. 异常场景测试
 * 4. 并发安全测试
 * 5. 性能测试
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HashCartServiceTest {

    @Autowired
    private HashCartService hashCartService;

    // 测试用的多个商品SKU ID
    private static final Long TEST_SKU_ID_1 = 1L;
    private static final Long TEST_SKU_ID_2 = 2L;
    private static final Long TEST_SKU_ID_3 = 3L;
    private static final Long TEST_SKU_ID_4 = 4L;
    private static final Long TEST_SKU_ID_5 = 5L;
    private static final List<Long> TEST_SKU_ID_LIST = Arrays.asList(TEST_SKU_ID_2, TEST_SKU_ID_3);

    @BeforeEach
    void setUpUserContext() {
        ClientUser clientUser = new ClientUser();
        clientUser.setId(10001L);
        clientUser.setMobile("13800000000");
        clientUser.setChannelId(1L);

        String tokenInfo = JSON.toJSONString(clientUser);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constant.USER_TOKEN_INFO, tokenInfo);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clearUserContext() {
        RequestContextHolder.resetRequestAttributes();
        AuthorizationContext.clear();
    }

    /**
     * 格式化价格显示，将分转换为元
     */
    private String formatPrice(long priceInCents) {
        return MoneyUtils.fenToYuanStr(priceInCents);
    }

    /**
     * 打印购物车详情
     */
    private void printCartDetails(Cart cart, String title) {
        System.out.println("\n=== " + title + " ===");
        System.out.println("购物车信息:");
        System.out.println("- 商品种类数量: " + cart.getCountType());
        System.out.println("- 商品总数量: " + cart.getCountNum());
        System.out.println("- 购物车总金额: " + cart.getTotalAmount() + "分 (" + formatPrice(cart.getTotalAmount()) + ")");

        if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
            System.out.println("购物车商品明细:");
            for (int i = 0; i < cart.getCartItems().size(); i++) {
                CartItem item = cart.getCartItems().get(i);
                System.out.println("  " + (i + 1) + ". SKU_ID: " + item.getSkuId() +
                        ", 数量: " + item.getCount() +
                        ", 单价: " + item.getPrice() + "分 (" + formatPrice(item.getPrice()) + ")" +
                        ", 小计: " + item.getTotalPrice() + "分 (" + formatPrice(item.getTotalPrice()) + ")" +
                        ", 是否勾选: " + item.isChecked());
            }
        } else {
            System.out.println("购物车为空");
        }
    }

    // ========== 基本功能测试 ==========

    @Test
    @Order(1)
    @DisplayName("01-上下文加载测试")
    void contextLoads() {
        assertNotNull(hashCartService, "HashCartService 未注入，检查是否存在实现与Spring扫描配置");
        System.out.println("✓ 购物车服务注入成功");
    }

    @Test
    @Order(2)
    @DisplayName("02-添加单个商品到购物车")
    void testAddToCart() {
        System.out.println("\n=== 测试添加单个商品到购物车 ===");
        boolean result = hashCartService.addToCart(TEST_SKU_ID_1, 1);
        assertTrue(result, "添加购物车应返回true");
        
        // 验证添加结果
        Cart cart = hashCartService.userCartList();
        assertEquals(1, cart.getCountType(), "购物车应有1种商品");
        assertEquals(1, cart.getCountNum(), "购物车应有1件商品");
        System.out.println("✓ 添加单个商品成功");
    }

    @Test
    @Order(3)
    @DisplayName("03-重复添加相同商品（累加数量）")
    void testAddSameItem() {
        System.out.println("\n=== 测试重复添加相同商品 ===");
        // 再次添加相同商品
        hashCartService.addToCart(TEST_SKU_ID_1, 2);
        
        Cart cart = hashCartService.userCartList();
        assertEquals(1, cart.getCountType(), "购物车应仍为1种商品");
        assertEquals(3, cart.getCountNum(), "购物车商品数量应累加为3");
        
        CartItem item = cart.getCartItems().stream()
                .filter(i -> TEST_SKU_ID_1.equals(i.getSkuId()))
                .findFirst()
                .orElse(null);
        assertNotNull(item, "应能找到该商品");
        assertEquals(3, item.getCount(), "商品数量应为3");
        System.out.println("✓ 重复添加商品成功，数量已累加");
    }

    @Test
    @Order(4)
    @DisplayName("04-添加多个不同商品")
    void testAddMultipleItemsToCart() {
        System.out.println("\n=== 测试添加多个不同商品到购物车 ===");
        // 添加第二个商品
        hashCartService.addToCart(TEST_SKU_ID_2, 2);
        // 添加第三个商品
        hashCartService.addToCart(TEST_SKU_ID_3, 3);
        
        Cart cart = hashCartService.userCartList();
        assertEquals(3, cart.getCountType(), "购物车应有3种商品");
        assertEquals(8, cart.getCountNum(), "购物车应有8件商品");
        System.out.println("✓ 添加多个商品成功");
    }

    @Test
    @Order(5)
    @DisplayName("05-查询购物车列表")
    void testUserCartList() {
        Cart cart = hashCartService.userCartList();
        assertNotNull(cart, "用户购物车列表不应为null");
        assertNotNull(cart.getCartItems(), "购物车items不应为null");
        
        printCartDetails(cart, "当前购物车状态");
        System.out.println("✓ 查询购物车成功");
    }

    @Test
    @Order(6)
    @DisplayName("06-修改购物车商品数量")
    void testModifyItemCount() {
        System.out.println("\n=== 测试修改购物车商品数量 ===");
        boolean ok = hashCartService.modifyItemCount(TEST_SKU_ID_1, 5);
        assertTrue(ok, "修改购物车商品数量应返回true");
        
        // 验证修改结果
        Cart cart = hashCartService.userCartList();
        CartItem item = cart.getCartItems().stream()
                .filter(i -> TEST_SKU_ID_1.equals(i.getSkuId()))
                .findFirst()
                .orElse(null);
        assertNotNull(item, "应能找到该商品");
        assertEquals(5, item.getCount(), "商品数量应修改为5");
        System.out.println("✓ 修改商品数量成功");
    }

    @Test
    @Order(7)
    @DisplayName("07-勾选/取消勾选购物项")
    void testCheckItem() {
        System.out.println("\n=== 测试勾选购物项 ===");
        
        // 获取初始状态
        Cart cartBefore = hashCartService.userCartList();
        CartItem itemBefore = cartBefore.getCartItems().stream()
                .filter(i -> TEST_SKU_ID_1.equals(i.getSkuId()))
                .findFirst()
                .orElse(null);
        assertNotNull(itemBefore, "商品应存在");
        boolean initialChecked = itemBefore.isChecked();
        System.out.println("初始勾选状态: " + initialChecked);
        
        // 切换勾选状态
        boolean ok = hashCartService.checkItem(TEST_SKU_ID_1);
        assertTrue(ok, "勾选购物项应返回true");
        
        // 验证状态已切换
        Cart cartAfter = hashCartService.userCartList();
        CartItem itemAfter = cartAfter.getCartItems().stream()
                .filter(i -> TEST_SKU_ID_1.equals(i.getSkuId()))
                .findFirst()
                .orElse(null);
        assertNotNull(itemAfter, "商品应存在");
        assertEquals(!initialChecked, itemAfter.isChecked(), "勾选状态应已切换");
        System.out.println("切换后勾选状态: " + itemAfter.isChecked());
        System.out.println("✓ 勾选商品成功");
    }

    @Test
    @Order(8)
    @DisplayName("08-批量删除购物项")
    void testBatchDeleteItem() {
        System.out.println("\n=== 测试批量删除购物项 ===");
        
        // 删除前先查看购物车状态
        Cart cartBefore = hashCartService.userCartList();
        int sizeBefore = cartBefore.getCartItems() != null ? cartBefore.getCartItems().size() : 0;
        System.out.println("删除前购物车商品数量: " + sizeBefore);
        System.out.println("准备删除的商品SKU列表: " + TEST_SKU_ID_LIST);
        
        boolean ok = hashCartService.batchDeleteItem(TEST_SKU_ID_LIST);
        assertTrue(ok, "批量删除购物项应返回true");
        
        // 删除后查看购物车状态
        Cart cartAfter = hashCartService.userCartList();
        int sizeAfter = cartAfter.getCartItems() != null ? cartAfter.getCartItems().size() : 0;
        System.out.println("删除后购物车商品数量: " + sizeAfter);
        assertEquals(sizeBefore - 2, sizeAfter, "应删除2个商品");
        
        // 验证指定商品已删除
        boolean hasDeleted = cartAfter.getCartItems().stream()
                .anyMatch(item -> TEST_SKU_ID_LIST.contains(item.getSkuId()));
        assertFalse(hasDeleted, "被删除的商品不应再存在");
        System.out.println("✓ 批量删除成功");
    }

    @Test
    @Order(9)
    @DisplayName("09-删除单个购物项")
    void testDeleteItem() {
        System.out.println("\n=== 测试删除单个购物项 ===");
        
        // 删除前先查看购物车状态
        Cart cartBefore = hashCartService.userCartList();
        int sizeBefore = cartBefore.getCartItems() != null ? cartBefore.getCartItems().size() : 0;
        System.out.println("删除前购物车商品数量: " + sizeBefore);
        System.out.println("准备删除的商品SKU: " + TEST_SKU_ID_1);
        
        boolean ok = hashCartService.deleteItem(TEST_SKU_ID_1);
        assertTrue(ok, "删除购物项应返回true");
        
        // 删除后查看购物车状态
        Cart cartAfter = hashCartService.userCartList();
        int sizeAfter = cartAfter.getCartItems() != null ? cartAfter.getCartItems().size() : 0;
        System.out.println("删除后购物车商品数量: " + sizeAfter);
        assertEquals(sizeBefore - 1, sizeAfter, "应删除1个商品");
        
        // 验证商品已删除
        boolean exists = cartAfter.getCartItems().stream()
                .anyMatch(item -> TEST_SKU_ID_1.equals(item.getSkuId()));
        assertFalse(exists, "被删除的商品不应再存在");
        System.out.println("✓ 删除单个商品成功");
    }

    @Test
    @Order(10)
    @DisplayName("10-查看最终购物车状态")
    void testFinalCartList() {
        Cart cart = hashCartService.userCartList();
        assertNotNull(cart, "用户购物车列表不应为null");
        printCartDetails(cart, "最终购物车状态");
    }

    @Test
    @Order(11)
    @DisplayName("11-清空用户购物车")
    void testClearUserCart() {
        System.out.println("\n=== 测试清空用户购物车 ===");
        
        // 清空前先查看购物车状态
        Cart cartBefore = hashCartService.userCartList();
        int sizeBefore = cartBefore.getCartItems() != null ? cartBefore.getCartItems().size() : 0;
        System.out.println("清空前购物车商品数量: " + sizeBefore);
        
        boolean ok = hashCartService.clearUserCart();
        assertTrue(ok, "清空购物车应返回true");
        
        // 清空后查看购物车状态
        Cart cartAfter = hashCartService.userCartList();
        int sizeAfter = cartAfter.getCartItems() != null ? cartAfter.getCartItems().size() : 0;
        assertEquals(0, sizeAfter, "清空后购物车应为空");
        System.out.println("清空后购物车商品数量: " + sizeAfter);
        System.out.println("✓ 购物车已清空");
    }

    // ========== 边界值测试 ==========

    @Test
    @Order(20)
    @DisplayName("20-测试添加数量为0的商品（异常）")
    void testAddZeroCount() {
        System.out.println("\n=== 测试添加数量为0的商品 ===");
        ApiException exception = assertThrows(ApiException.class, () -> hashCartService.addToCart(TEST_SKU_ID_4, 0));
        System.out.println("预期异常: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("必须大于0"));
        System.out.println("✓ 正确拒绝了非法数量");
    }

    @Test
    @Order(21)
    @DisplayName("21-测试添加负数数量的商品（异常）")
    void testAddNegativeCount() {
        System.out.println("\n=== 测试添加负数数量的商品 ===");
        ApiException exception = assertThrows(ApiException.class, () -> hashCartService.addToCart(TEST_SKU_ID_4, -1));
        System.out.println("预期异常: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("必须大于0"));
        System.out.println("✓ 正确拒绝了负数数量");
    }

    @Test
    @Order(22)
    @DisplayName("22-测试添加超限数量的商品（异常）")
    void testAddExceedMaxCount() {
        System.out.println("\n=== 测试添加超限数量的商品 ===");
        ApiException exception = assertThrows(ApiException.class, () -> {
            hashCartService.addToCart(TEST_SKU_ID_4, 101); // 假设maxAddNumber为100
        });
        System.out.println("预期异常: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("不能超过"));
        System.out.println("✓ 正确拒绝了超限数量");
    }

    @Test
    @Order(23)
    @DisplayName("23-测试修改商品数量为0（异常）")
    void testModifyCountToZero() {
        System.out.println("\n=== 测试修改商品数量为0 ===");
        // 先添加商品
        hashCartService.addToCart(TEST_SKU_ID_4, 1);
        
        // 尝试修改为0
        ApiException exception = assertThrows(ApiException.class, () -> hashCartService.modifyItemCount(TEST_SKU_ID_4, 0));
        System.out.println("预期异常: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("必须大于0"));
        
        // 清理
        hashCartService.deleteItem(TEST_SKU_ID_4);
        System.out.println("✓ 正确拒绝了修改为0");
    }

    @Test
    @Order(24)
    @DisplayName("24-测试操作不存在的商品（异常）")
    void testOperateNonExistItem() {
        System.out.println("\n=== 测试操作不存在的商品 ===");
        
        Long nonExistSkuId = 99999L;
        
        // 测试修改不存在的商品
        ApiException exception1 = assertThrows(ApiException.class, () -> hashCartService.modifyItemCount(nonExistSkuId, 5));
        System.out.println("修改不存在商品异常: " + exception1.getMessage());
        assertTrue(exception1.getMessage().contains("不存在"));
        
        // 测试勾选不存在的商品
        ApiException exception2 = assertThrows(ApiException.class, () -> hashCartService.checkItem(nonExistSkuId));
        System.out.println("勾选不存在商品异常: " + exception2.getMessage());
        assertTrue(exception2.getMessage().contains("不存在"));
        
        System.out.println("✓ 正确处理了不存在的商品");
    }

    @Test
    @Order(25)
    @DisplayName("25-测试空参数（异常）")
    void testNullParams() {
        System.out.println("\n=== 测试空参数 ===");
        
        // 测试空skuId
        ApiException exception1 = assertThrows(ApiException.class, () -> hashCartService.addToCart(null, 1));
        System.out.println("空skuId异常: " + exception1.getMessage());
        assertTrue(exception1.getMessage().contains("不能为空"));
        
        // 测试空数量
        ApiException exception2 = assertThrows(ApiException.class, () -> hashCartService.addToCart(TEST_SKU_ID_5, null));
        System.out.println("空数量异常: " + exception2.getMessage());
        assertTrue(exception2.getMessage().contains("必须大于0"));
        
        System.out.println("✓ 正确处理了空参数");
    }

    // ========== 并发安全测试 ==========

    @Test
    @Order(30)
    @DisplayName("30-并发添加相同商品测试")
    void testConcurrentAddSameItem() throws InterruptedException {
        System.out.println("\n=== 并发添加相同商品测试 ===");
        
        // 先清空购物车
        hashCartService.clearUserCart();
        
        int threadCount = 10;
        int addCountPerThread = 5;
        ExecutorService executorService = ThreadUtil.initFixedThreadPoolExecutor("test", threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    hashCartService.addToCart(TEST_SKU_ID_1, addCountPerThread);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("并发添加失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        System.out.println("并发添加完成: 成功=" + successCount.get() + ", 失败=" + failCount.get());

        // 验证最终结果
        Cart cart = hashCartService.userCartList();
        CartItem item = cart.getCartItems().stream()
                .filter(i -> TEST_SKU_ID_1.equals(i.getSkuId()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(item, "商品应存在");
        int expectedCount = threadCount * addCountPerThread;
        assertEquals(expectedCount, item.getCount(), 
                "并发添加后数量应为" + expectedCount);
        System.out.println("✓ 并发测试通过，最终数量: " + item.getCount());
        
        // 清理
        hashCartService.clearUserCart();
    }

    @Test
    @Order(31)
    @DisplayName("31-并发修改和查询测试")
    void testConcurrentModifyAndQuery() throws InterruptedException {
        System.out.println("\n=== 并发修改和查询测试 ===");
        
        // 准备测试数据
        hashCartService.clearUserCart();
        hashCartService.addToCart(TEST_SKU_ID_1, 10);
        hashCartService.addToCart(TEST_SKU_ID_2, 20);

        int threadCount = 20;
        ExecutorService executorService = ThreadUtil.initFixedThreadPoolExecutor("test", threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger queryCount = new AtomicInteger(0);
        AtomicInteger modifyCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // 查询操作
                        Cart cart = hashCartService.userCartList();
                        assertNotNull(cart);
                        queryCount.incrementAndGet();
                    } else {
                        // 修改操作
                        hashCartService.modifyItemCount(TEST_SKU_ID_1, 15);
                        modifyCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("并发操作失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        System.out.println("并发操作完成: 查询=" + queryCount.get() + ", 修改=" + modifyCount.get());
        System.out.println("✓ 并发修改和查询测试通过");
        
        // 清理
        hashCartService.clearUserCart();
    }

    // ========== 性能测试 ==========

    @Test
    @Order(40)
    @DisplayName("40-批量添加性能测试")
    void testBatchAddPerformance() {
        System.out.println("\n=== 批量添加性能测试 ===");
        
        // 先清空购物车
        hashCartService.clearUserCart();
        
        int itemCount = 50;
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= itemCount; i++) {
            hashCartService.addToCart((long) i, 1);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Cart cart = hashCartService.userCartList();
        assertEquals(itemCount, cart.getCountType(), "应有" + itemCount + "种商品");
        
        System.out.println("添加 " + itemCount + " 个商品耗时: " + duration + "ms");
        System.out.println("平均每个商品耗时: " + (duration * 1.0 / itemCount) + "ms");
        System.out.println("✓ 批量添加性能测试完成");
        
        // 清理
        hashCartService.clearUserCart();
    }

    @Test
    @Order(41)
    @DisplayName("41-大购物车查询性能测试")
    void testLargeCartQueryPerformance() {
        System.out.println("\n=== 大购物车查询性能测试 ===");
        
        // 先清空并准备数据
        hashCartService.clearUserCart();
        int itemCount = 80;
        
        for (int i = 1; i <= itemCount; i++) {
            hashCartService.addToCart((long) i, i);
        }
        
        // 测试查询性能
        int queryTimes = 10;
        long totalDuration = 0;
        
        for (int i = 0; i < queryTimes; i++) {
            long startTime = System.currentTimeMillis();
            Cart cart = hashCartService.userCartList();
            long endTime = System.currentTimeMillis();
            totalDuration += (endTime - startTime);
            
            assertNotNull(cart);
            assertEquals(itemCount, cart.getCountType());
        }
        
        double avgDuration = totalDuration * 1.0 / queryTimes;
        System.out.println("查询 " + itemCount + " 个商品的购物车");
        System.out.println("执行 " + queryTimes + " 次查询，总耗时: " + totalDuration + "ms");
        System.out.println("平均查询耗时: " + avgDuration + "ms");
        System.out.println("✓ 大购物车查询性能测试完成");
        
        // 清理
        hashCartService.clearUserCart();
    }

    @Test
    @Order(42)
    @DisplayName("42-批量删除性能测试")
    void testBatchDeletePerformance() {
        System.out.println("\n=== 批量删除性能测试 ===");
        
        // 准备测试数据
        hashCartService.clearUserCart();
        int itemCount = 50;
        List<Long> skuIds = new java.util.ArrayList<>();
        
        for (int i = 1; i <= itemCount; i++) {
            hashCartService.addToCart((long) i, 1);
            if (i <= 30) {
                skuIds.add((long) i);
            }
        }
        
        // 测试批量删除性能
        long startTime = System.currentTimeMillis();
        hashCartService.batchDeleteItem(skuIds);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Cart cart = hashCartService.userCartList();
        assertEquals(itemCount - skuIds.size(), cart.getCountType(), 
                "应剩余" + (itemCount - skuIds.size()) + "种商品");
        
        System.out.println("批量删除 " + skuIds.size() + " 个商品耗时: " + duration + "ms");
        System.out.println("✓ 批量删除性能测试完成");
        
        // 清理
        hashCartService.clearUserCart();
    }

    // ========== 综合测试 ==========

    @Test
    @Order(50)
    @DisplayName("50-完整业务流程测试")
    void testCompleteBusinessFlow() {
        System.out.println("\n=== 完整业务流程测试 ===");
        
        // 1. 清空购物车
        hashCartService.clearUserCart();
        System.out.println("1. 清空购物车完成");
        
        // 2. 添加多个商品
        hashCartService.addToCart(TEST_SKU_ID_1, 2);
        hashCartService.addToCart(TEST_SKU_ID_2, 3);
        hashCartService.addToCart(TEST_SKU_ID_3, 1);
        System.out.println("2. 添加3种商品完成");
        
        // 3. 查询购物车
        Cart cart1 = hashCartService.userCartList();
        assertEquals(3, cart1.getCountType());
        assertEquals(6, cart1.getCountNum());
        System.out.println("3. 查询购物车: 共" + cart1.getCountType() + "种商品");
        
        // 4. 修改商品数量
        hashCartService.modifyItemCount(TEST_SKU_ID_1, 5);
        System.out.println("4. 修改商品数量完成");
        
        // 5. 取消勾选商品
        hashCartService.checkItem(TEST_SKU_ID_2);
        System.out.println("5. 取消勾选商品完成");
        
        // 6. 再次查询验证
        Cart cart2 = hashCartService.userCartList();
        assertEquals(3, cart2.getCountType());
        System.out.println("6. 再次查询: 商品种类=" + cart2.getCountType());
        
        // 7. 删除单个商品
        hashCartService.deleteItem(TEST_SKU_ID_3);
        System.out.println("7. 删除单个商品完成");
        
        // 8. 最终验证
        Cart cart3 = hashCartService.userCartList();
        assertEquals(2, cart3.getCountType());
        System.out.println("8. 最终验证: 剩余" + cart3.getCountType() + "种商品");
        
        printCartDetails(cart3, "完整流程测试结果");
        System.out.println("✓ 完整业务流程测试通过");
        
        // 清理
        hashCartService.clearUserCart();
    }

    @Test
    @Order(99)
    @DisplayName("99-最终清理")
    void finalCleanup() {
        System.out.println("\n=== 最终清理 ===");
        hashCartService.clearUserCart();
        Cart cart = hashCartService.userCartList();
        assertEquals(0, cart.getCountType(), "购物车应为空");
        System.out.println("✓ 所有测试完成，购物车已清空");
    }
}
