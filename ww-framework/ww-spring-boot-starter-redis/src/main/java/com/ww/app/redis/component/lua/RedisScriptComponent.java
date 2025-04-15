package com.ww.app.redis.component.lua;

import cn.hutool.core.lang.Assert;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-12-29 14:59
 * @description: Redis脚本组件，用于预加载和管理Lua脚本
 */
@Slf4j
@Component
public class RedisScriptComponent {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 脚本SHA缓存，key为脚本名称，value为脚本SHA
     */
    private final Map<String, byte[]> scriptShaCache = new ConcurrentHashMap<>();

    /**
     * 加载脚本到Redis服务器
     *
     * @param scriptName 脚本名称
     * @param script 脚本内容
     */
    public void preLoadLuaScript(String scriptName, String script) {
        try {
            String sha = stringRedisTemplate.execute(connection ->
                    connection.scriptLoad(script.getBytes(StandardCharsets.UTF_8)), true);
            Assert.notNull(sha, () -> new ApiException("预加载lua脚本结果为null"));
            scriptShaCache.put(scriptName, sha.getBytes(StandardCharsets.UTF_8));
            log.info("脚本加载成功: name={}, sha={}", scriptName, sha);
        } catch (Exception e) {
            log.error("脚本加载异常: name={}", scriptName, e);
            throw new RuntimeException("脚本加载失败: " + scriptName, e);
        }
    }

    /**
     * 获取脚本SHA
     *
     * @param scriptName 脚本名称
     * @return 脚本SHA
     */
    public byte[] getLuaScriptSha(String scriptName) {
        return scriptShaCache.get(scriptName);
    }

    /**
     * 执行脚本（底层方法，需要调用方处理序列化）
     *
     * @param scriptName 脚本名称
     * @param returnType 返回值类型
     * @param numKeys 键的数量
     * @param keyAndArgs 键值列表
     * @return 执行结果
     */
    public <T> T executeLuaScript(String scriptName, ReturnType returnType, int numKeys, byte[]... keyAndArgs) {
        byte[] luaScriptSha = getLuaScriptSha(scriptName);
        if (luaScriptSha == null) {
            throw new ApiException("脚本未预加载");
        }
        return stringRedisTemplate.execute(connection ->
            connection.evalSha(luaScriptSha, returnType, numKeys, keyAndArgs), true);
    }

    /**
     * 执行脚本（简化版，自动处理字符串序列化）
     *
     * @param scriptName 脚本名称
     * @param keys Redis键列表
     * @param args 参数列表
     * @return 执行结果（Long类型）
     */
    public <T> T executeLuaScript(String scriptName, ReturnType returnType, List<String> keys, List<String> args) {
        // 准备字节数组
        byte[][] keyAndArgBytes = prepareKeyAndArgBytes(keys, args);
        // 执行脚本
        return executeLuaScript(scriptName, returnType, keys.size(), keyAndArgBytes);
    }
    
    /**
     * 准备键和参数的字节数组
     */
    private byte[][] prepareKeyAndArgBytes(List<String> keys, List<String> args) {
        int totalLength = keys.size() + (args != null ? args.size() : 0);
        byte[][] keyAndArgBytes = new byte[totalLength][];
        
        // 添加键
        for (int i = 0; i < keys.size(); i++) {
            keyAndArgBytes[i] = keys.get(i).getBytes(StandardCharsets.UTF_8);
        }
        
        // 添加参数
        if (args != null && !args.isEmpty()) {
            for (int i = 0; i < args.size(); i++) {
                keyAndArgBytes[keys.size() + i] = args.get(i).getBytes(StandardCharsets.UTF_8);
            }
        }
        
        return keyAndArgBytes;
    }
} 