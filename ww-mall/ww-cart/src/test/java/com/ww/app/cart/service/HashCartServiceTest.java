package com.ww.app.cart.service;

import com.alibaba.fastjson.JSON;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.utils.MoneyUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    }

    /**
     * 格式化价格显示，将分转换为元
     */
    private String formatPrice(long priceInCents) {
        return MoneyUtils.fenToYuanStr(priceInCents);
    }

    @Test
    @Order(1)
    void contextLoads() {
        assertNotNull(hashCartService, "HashCartService 未注入，检查是否存在实现与Spring扫描配置");
    }

    @Test
    @Order(2)
    void testAddToCart() {
        System.out.println("=== 测试添加单个商品到购物车 ===");
        CartItem item = hashCartService.addToCart(TEST_SKU_ID_1, 1);
        assertNotNull(item, "添加购物车应返回CartItem");
        assertEquals(TEST_SKU_ID_1, item.getSkuId(), "返回的SKU不一致");
        System.out.println("添加商品成功: SKU_ID=" + item.getSkuId() + ", 数量=" + item.getCount());
    }

    @Test
    @Order(3)
    void testAddMultipleItemsToCart() {
        System.out.println("=== 测试添加多个商品到购物车 ===");
        
        // 添加第二个商品
        CartItem item2 = hashCartService.addToCart(TEST_SKU_ID_2, 2);
        assertNotNull(item2, "添加第二个商品应返回CartItem");
        assertEquals(TEST_SKU_ID_2, item2.getSkuId(), "第二个商品SKU不一致");
        System.out.println("添加第二个商品成功: SKU_ID=" + item2.getSkuId() + ", 数量=" + item2.getCount());
        
        // 添加第三个商品
        CartItem item3 = hashCartService.addToCart(TEST_SKU_ID_3, 3);
        assertNotNull(item3, "添加第三个商品应返回CartItem");
        assertEquals(TEST_SKU_ID_3, item3.getSkuId(), "第三个商品SKU不一致");
        System.out.println("添加第三个商品成功: SKU_ID=" + item3.getSkuId() + ", 数量=" + item3.getCount());
    }

    @Test
    @Order(4)
    void testUserCartList() {
        System.out.println("=== 测试获取用户购物车列表 ===");
        Cart cart = hashCartService.userCartList();
        assertNotNull(cart, "用户购物车列表不应为null");
        assertNotNull(cart.getCartItems(), "购物车items不应为null");
        
        System.out.println("购物车信息:");
        System.out.println("- 商品种类数量: " + cart.getCountType());
        System.out.println("- 商品总数量: " + cart.getCountNum());
        System.out.println("- 购物车总金额: " + cart.getTotalAmount() + "分 (" + formatPrice(cart.getTotalAmount()) + ")");
        System.out.println("- 扣减金额: " + cart.getReduceAmount() + "分 (" + formatPrice(cart.getReduceAmount()) + ")");
        
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

    @Test
    @Order(5)
    void testModifyItemCount() {
        System.out.println("=== 测试修改购物车商品数量 ===");
        boolean ok = hashCartService.modifyItemCount(TEST_SKU_ID_1, 5);
        assertTrue(ok, "修改购物车商品数量应返回true");
        System.out.println("修改商品数量成功: SKU_ID=" + TEST_SKU_ID_1 + ", 新数量=5");
        
        // 再次查看购物车列表验证修改结果
        Cart cart = hashCartService.userCartList();
        if (cart.getCartItems() != null) {
            cart.getCartItems().stream()
                .filter(item -> TEST_SKU_ID_1.equals(item.getSkuId()))
                .forEach(item -> System.out.println("修改后的商品: SKU_ID=" + item.getSkuId() + ", 数量=" + item.getCount()));
        }
    }

    @Test
    @Order(6)
    void testCheckItem() {
        System.out.println("=== 测试勾选购物项 ===");
        boolean ok = hashCartService.checkItem(TEST_SKU_ID_1);
        assertTrue(ok, "勾选购物项应返回true");
        System.out.println("勾选商品成功: SKU_ID=" + TEST_SKU_ID_1);
        
        // 再次查看购物车列表验证勾选结果
        Cart cart = hashCartService.userCartList();
        if (cart.getCartItems() != null) {
            cart.getCartItems().stream()
                .filter(item -> TEST_SKU_ID_1.equals(item.getSkuId()))
                .forEach(item -> System.out.println("勾选后的商品: SKU_ID=" + item.getSkuId() + ", 是否勾选=" + item.isChecked()));
        }
    }

    @Test
    @Order(7)
    void testBatchDeleteItem() {
        System.out.println("=== 测试批量删除购物项 ===");
        System.out.println("准备删除的商品SKU列表: " + TEST_SKU_ID_LIST);
        
        // 删除前先查看购物车状态
        Cart cartBefore = hashCartService.userCartList();
        System.out.println("删除前购物车商品数量: " + (cartBefore.getCartItems() != null ? cartBefore.getCartItems().size() : 0));
        
        boolean ok = hashCartService.batchDeleteItem(TEST_SKU_ID_LIST);
        assertTrue(ok, "批量删除购物项应返回true");
        System.out.println("批量删除操作完成");
        
        // 删除后查看购物车状态
        Cart cartAfter = hashCartService.userCartList();
        System.out.println("删除后购物车商品数量: " + (cartAfter.getCartItems() != null ? cartAfter.getCartItems().size() : 0));
        
        if (cartAfter.getCartItems() != null && !cartAfter.getCartItems().isEmpty()) {
            System.out.println("剩余商品:");
            cartAfter.getCartItems().forEach(item -> 
                System.out.println("  - SKU_ID: " + item.getSkuId() + ", 数量: " + item.getCount()));
        }
    }

    @Test
    @Order(8)
    void testDeleteItem() {
        System.out.println("=== 测试删除单个购物项 ===");
        System.out.println("准备删除的商品SKU: " + TEST_SKU_ID_1);
        
        // 删除前先查看购物车状态
        Cart cartBefore = hashCartService.userCartList();
        System.out.println("删除前购物车商品数量: " + (cartBefore.getCartItems() != null ? cartBefore.getCartItems().size() : 0));
        
        boolean ok = hashCartService.deleteItem(TEST_SKU_ID_1);
        assertTrue(ok, "删除购物项应返回true");
        System.out.println("删除操作完成");
        
        // 删除后查看购物车状态
        Cart cartAfter = hashCartService.userCartList();
        System.out.println("删除后购物车商品数量: " + (cartAfter.getCartItems() != null ? cartAfter.getCartItems().size() : 0));
    }

    @Test
    @Order(9)
    void testFinalCartList() {
        System.out.println("=== 最终购物车状态 ===");
        Cart cart = hashCartService.userCartList();
        assertNotNull(cart, "用户购物车列表不应为null");
        
        System.out.println("最终购物车信息:");
        System.out.println("- 商品种类数量: " + cart.getCountType());
        System.out.println("- 商品总数量: " + cart.getCountNum());
        System.out.println("- 购物车总金额: " + cart.getTotalAmount() + "分 (" + formatPrice(cart.getTotalAmount()) + ")");
        System.out.println("- 扣减金额: " + cart.getReduceAmount() + "分 (" + formatPrice(cart.getReduceAmount()) + ")");
        
        if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
            System.out.println("最终购物车商品明细:");
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

    @Test
    @Order(10)
    void testClearUserCart() {
        System.out.println("=== 测试清空用户购物车 ===");
        
        // 清空前先查看购物车状态
        Cart cartBefore = hashCartService.userCartList();
        System.out.println("清空前购物车商品数量: " + (cartBefore.getCartItems() != null ? cartBefore.getCartItems().size() : 0));
        
        boolean ok = hashCartService.clearUserCart();
        assertTrue(ok, "清空购物车应返回true");
        System.out.println("清空购物车操作完成");
        
        // 清空后查看购物车状态
        Cart cartAfter = hashCartService.userCartList();
        System.out.println("清空后购物车商品数量: " + (cartAfter.getCartItems() != null ? cartAfter.getCartItems().size() : 0));
        System.out.println("购物车已清空");
    }
}


