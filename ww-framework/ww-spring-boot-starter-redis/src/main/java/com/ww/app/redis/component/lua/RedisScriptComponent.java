package com.ww.app.redis.component.lua;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-12-29 14:59
 * @description: Redis 脚本组件，用于预加载和管理 Lua 脚本
 * </p>
 * 注意事项：
 * - 脚本可能被重试或重复执行，调用方需保证幂等。
 * - 发生 NOSCRIPT 时会自动重载脚本。
 */
@Slf4j
@Component
public class RedisScriptComponent {

    private static final String NOSCRIPT_ERROR = "NOSCRIPT";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 脚本缓存，key 为脚本名称，value 为脚本元信息。
     */
    private final Map<String, ScriptMeta> scriptCache = new ConcurrentHashMap<>();

    /**
     * 预加载脚本到 Redis 服务器。
     *
     * @param scriptName 脚本名称
     * @param script 脚本内容
     */
    public void preLoadLuaScript(String scriptName, String script) {
        Assert.notBlank(scriptName, () -> new ApiException("脚本名称不能为空"));
        Assert.notBlank(script, () -> new ApiException("脚本内容不能为空"));
        preLoadLuaScript(scriptName, script.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 预加载脚本到 Redis 服务器。
     *
     * @param scriptName 脚本名称
     * @param scriptBytes 脚本字节
     */
    public void preLoadLuaScript(String scriptName, byte[] scriptBytes) {
        Assert.notBlank(scriptName, () -> new ApiException("脚本名称不能为空"));
        Assert.notNull(scriptBytes, () -> new ApiException("脚本内容不能为空"));
        Assert.isTrue(scriptBytes.length > 0, () -> new ApiException("脚本内容不能为空"));
        try {
            ScriptMeta scriptMeta = new ScriptMeta(scriptBytes);
            byte[] shaBytes = stringRedisTemplate.execute(connection ->
                    loadScript(connection, scriptBytes), true);
            Assert.notNull(shaBytes, () -> new ApiException("预加载 lua 脚本结果为 null"));
            scriptMeta.shaBytes = shaBytes;
            scriptCache.put(scriptName, scriptMeta);
            log.info("脚本加载成功: name={}, sha={}", scriptName, new String(shaBytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("脚本加载异常: name={}", scriptName, e);
            throw new RuntimeException("脚本加载失败: " + scriptName, e);
        }
    }

    /**
     * 获取脚本 SHA。
     *
     * @param scriptName 脚本名称
     * @return 脚本 SHA
     */
    public byte[] getLuaScriptSha(String scriptName) {
        ScriptMeta scriptMeta = scriptCache.get(scriptName);
        return scriptMeta == null ? null : scriptMeta.shaBytes;
    }

    /**
     * 执行脚本（底层方法，需要调用方处理序列化）。
     *
     * @param scriptName 脚本名称
     * @param returnType 返回值类型
     * @param numKeys 键的数量
     * @param keyAndArgs 键值列表
     * @return 执行结果
     */
    public <T> T executeLuaScript(String scriptName, ReturnType returnType, int numKeys, byte[]... keyAndArgs) {
        ScriptMeta scriptMeta = getScriptMeta(scriptName);
        if (scriptMeta == null) {
            throw new ApiException("脚本未预加载: " + scriptName);
        }
        return stringRedisTemplate.execute(connection -> {
            byte[] shaBytes = scriptMeta.shaBytes;
            if (shaBytes == null) {
                shaBytes = loadScript(connection, scriptMeta.scriptBytes);
                scriptMeta.shaBytes = shaBytes;
            }
            try {
                return connection.evalSha(shaBytes, returnType, numKeys, keyAndArgs);
            } catch (Exception ex) {
                if (isNoScriptError(ex)) {
                    byte[] reloadSha = loadScript(connection, scriptMeta.scriptBytes);
                    scriptMeta.shaBytes = reloadSha;
                    return connection.evalSha(reloadSha, returnType, numKeys, keyAndArgs);
                }
                throw ex;
            }
        }, true);
    }

    /**
     * 执行脚本（简化版，自动处理字符串序列化）。
     *
     * @param scriptName 脚本名称
     * @param returnType 返回值类型
     * @param keys Redis 键列表
     * @param args 参数列表
     * @return 执行结果
     */
    public <T> T executeLuaScript(String scriptName, ReturnType returnType, List<String> keys, List<String> args) {
        Assert.isTrue(CollectionUtil.isNotEmpty(keys), () -> new ApiException("keys 不能为空"));
        byte[][] keyAndArgBytes = prepareKeyAndArgBytes(keys, args);
        return executeLuaScript(scriptName, returnType, keys.size(), keyAndArgBytes);
    }

    /**
     * 准备键和参数的字节数组。
     *
     * @param keys Redis 键列表
     * @param args 参数列表
     * @return 字节数组
     */
    private byte[][] prepareKeyAndArgBytes(List<String> keys, List<String> args) {
        int keySize = keys.size();
        int argSize = args == null ? 0 : args.size();
        byte[][] keyAndArgBytes = new byte[keySize + argSize][];
        for (int i = 0; i < keySize; i++) {
            keyAndArgBytes[i] = serializeString(keys.get(i));
        }
        for (int i = 0; i < argSize; i++) {
            keyAndArgBytes[keySize + i] = serializeString(args.get(i));
        }
        return keyAndArgBytes;
    }

    /**
     * 获取脚本元信息。
     *
     * @param scriptName 脚本名称
     * @return 脚本元信息
     */
    private ScriptMeta getScriptMeta(String scriptName) {
        return scriptCache.get(scriptName);
    }

    /**
     * 加载脚本并返回 SHA。
     *
     * @param connection Redis 连接
     * @param scriptBytes 脚本字节
     * @return SHA 字节
     */
    private byte[] loadScript(RedisConnection connection, byte[] scriptBytes) {
        String sha = connection.scriptLoad(scriptBytes);
        Assert.notBlank(sha, () -> new ApiException("加载 lua 脚本结果为空"));
        return sha.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 判断是否为 NOSCRIPT 异常。
     *
     * @param ex 异常
     * @return 是否为 NOSCRIPT
     */
    private boolean isNoScriptError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(NOSCRIPT_ERROR)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 字符串序列化。
     *
     * @param value 字符串值
     * @return 字节数组
     */
    private byte[] serializeString(String value) {
        RedisSerializer<String> serializer = stringRedisTemplate.getStringSerializer();
        return serializer.serialize(value);
    }

    /**
     * 脚本元信息。
     */
    private static final class ScriptMeta {
        private final byte[] scriptBytes;
        private volatile byte[] shaBytes;

        private ScriptMeta(byte[] scriptBytes) {
            this.scriptBytes = scriptBytes;
        }
    }
}
