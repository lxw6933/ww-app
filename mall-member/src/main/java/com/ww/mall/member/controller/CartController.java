package com.ww.mall.member.controller;

import com.ww.mall.member.constant.CartConstant;
import com.ww.mall.member.interceptor.CartInterceptor;
import com.ww.mall.member.to.UserInfoTo;
import com.ww.mall.member.vo.CartItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description:
 * @author: ww
 * @create: 2021/7/3 下午9:27
 **/
@RestController
public class CartController {

//    @Resource
//    private RedisManager redisManager;

    /**
     * 添加购物车
     *
     * @param skuId 商品id
     * @param num 商品数量
     * @return R
     */
//    @GetMapping("addToCart")
//    public R addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
//        String cartKey = "";
//        if (userInfoTo.getUserId() == null) {
//            // 临时用户
//            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
//        } else {
//            // 登录用户
//            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
//        }
//        CartItemVO item = (CartItemVO) redisManager.hget(cartKey, skuId.toString());
//        if (item == null) {
//            // TODO 查询
//            CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> {
//                // 异步查询
//            }, threadPoolExecutor);
//            // 等待所有异步任务执行完成
//            CompletableFuture.allOf(task1);
//            item = new CartItemVO();
//            redisManager.hset(cartKey, skuId.toString(), item);
//        } else {
//            item.setCount(num);
//            redisManager.hset(cartKey, skuId.toString(), item);
//        }
//        return R.ok();
//    }

}
