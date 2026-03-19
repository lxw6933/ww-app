package com.ww.mall.promotion.engine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * 拼团 Lua 脚本配置。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 统一注册拼团状态机脚本
 */
@Configuration
public class GroupLuaScriptConfiguration {

    /**
     * 开团脚本。
     *
     * @return RedisScript
     */
    @Bean("groupCreateScript")
    public RedisScript<List<Object>> groupCreateScript() {
        return buildListScript("lua/group/create_group.lua");
    }

    /**
     * 参团脚本。
     *
     * @return RedisScript
     */
    @Bean("groupJoinScript")
    public RedisScript<List<Object>> groupJoinScript() {
        return buildListScript("lua/group/join_group.lua");
    }

    /**
     * 售后脚本。
     *
     * @return RedisScript
     */
    @Bean("groupAfterSaleScript")
    public RedisScript<List<Object>> groupAfterSaleScript() {
        return buildListScript("lua/group/after_sale_success.lua");
    }

    /**
     * 过期关团脚本。
     *
     * @return RedisScript
     */
    @Bean("groupExpireScript")
    public RedisScript<List<Object>> groupExpireScript() {
        return buildListScript("lua/group/expire_mark_failed.lua");
    }

    /**
     * 构建返回值为 List 的脚本对象。
     *
     * @param path 资源路径
     * @return RedisScript
     */
    private RedisScript<List<Object>> buildListScript(String path) {
        DefaultRedisScript<List<Object>> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource(path));
        redisScript.setResultType(listResultType());
        return redisScript;
    }

    /**
     * 获取 Lua 脚本 List 返回值的类型声明。
     * <p>
     * 由于 Java 泛型擦除，运行期只能拿到 {@code List.class}，
     * 这里集中处理一次受控转换，避免调用点出现原生类型警告。
     *
     * @return List<Object> 对应的 Class
     */
    @SuppressWarnings("unchecked")
    private Class<List<Object>> listResultType() {
        return (Class<List<Object>>) (Class<?>) List.class;
    }
}
