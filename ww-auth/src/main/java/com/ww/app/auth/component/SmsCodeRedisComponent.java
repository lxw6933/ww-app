package com.ww.app.auth.component;

import cn.hutool.core.util.StrUtil;
import com.ww.app.auth.component.key.SmsCodeRedisKeyBuilder;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * @author ww
 * @create 2026-02-13 16:30
 * @description: 短信验证码 Redis 组件。
 * <p>
 * 该组件统一管理验证码相关 Lua 脚本，保证以下并发语义：
 * 1. 发送验证码时，"频率校验 + 写入验证码" 为单条 Redis 原子操作，避免多实例并发下突破限频窗口。
 * 2. 登录校验时，"验证码比对 + 删除验证码" 为单条 Redis 原子操作，验证码一次性消费，防止重复使用。
 * 3. 短信发送失败时，支持按值比对删除，避免误删新请求写入的验证码。
 */
@Slf4j
@Component
public class SmsCodeRedisComponent {

    /**
     * 脚本名称：尝试写入验证码（包含限频校验）。
     */
    private static final String SCRIPT_TRY_SET_SMS_CODE = "auth_try_set_sms_code";

    /**
     * 脚本名称：校验并消费验证码。
     */
    private static final String SCRIPT_VALIDATE_AND_CONSUME_SMS_CODE = "auth_validate_consume_sms_code";

    /**
     * 脚本名称：按值匹配删除验证码。
     */
    private static final String SCRIPT_DELETE_IF_MATCH_SMS_CODE = "auth_delete_if_match_sms_code";

    /**
     * 发送验证码 Lua：
     * - 读取当前验证码值（格式：code_timestamp）；
     * - 若存在且未超过重发间隔，返回 0；
     * - 否则写入新值并设置过期时间，返回 1。
     */
    private static final String TRY_SET_SMS_CODE_LUA =
            "local value = redis.call('GET', KEYS[1])\n" +
            "local nowMillis = tonumber(ARGV[1])\n" +
            "local resendIntervalMillis = tonumber(ARGV[2])\n" +
            "local ttlSeconds = tonumber(ARGV[3])\n" +
            "local codeValue = ARGV[4]\n" +
            "if value then\n" +
            "  local delimiterIndex = string.find(value, '_')\n" +
            "  if delimiterIndex then\n" +
            "    local lastSendMillis = tonumber(string.sub(value, delimiterIndex + 1))\n" +
            "    if lastSendMillis and (nowMillis - lastSendMillis) < resendIntervalMillis then\n" +
            "      return 0\n" +
            "    end\n" +
            "  end\n" +
            "end\n" +
            "redis.call('SET', KEYS[1], codeValue, 'EX', ttlSeconds)\n" +
            "return 1";

    /**
     * 校验并消费验证码 Lua：
     * - 若 key 不存在，返回 0；
     * - 若验证码不匹配，返回 0；
     * - 若匹配，删除 key 并返回 1。
     */
    private static final String VALIDATE_AND_CONSUME_SMS_CODE_LUA =
            "local value = redis.call('GET', KEYS[1])\n" +
            "if not value then\n" +
            "  return 0\n" +
            "end\n" +
            "local inputCode = ARGV[1]\n" +
            "local delimiterIndex = string.find(value, '_')\n" +
            "local storedCode = value\n" +
            "if delimiterIndex then\n" +
            "  storedCode = string.sub(value, 1, delimiterIndex - 1)\n" +
            "end\n" +
            "if storedCode ~= inputCode then\n" +
            "  return 0\n" +
            "end\n" +
            "redis.call('DEL', KEYS[1])\n" +
            "return 1";

    /**
     * 按值匹配删除 Lua：
     * - 仅当当前 value 与入参 expectedValue 完全一致时删除，防止补偿误删新值。
     */
    private static final String DELETE_IF_MATCH_SMS_CODE_LUA =
            "local value = redis.call('GET', KEYS[1])\n" +
            "if value and value == ARGV[1] then\n" +
            "  return redis.call('DEL', KEYS[1])\n" +
            "end\n" +
            "return 0";

    @Resource
    private RedisScriptComponent redisScriptComponent;

    @Resource
    private SmsCodeRedisKeyBuilder smsCodeRedisKeyBuilder;

    /**
     * 组件初始化时预加载脚本，减少首次执行时延并规避运行期脚本缺失问题。
     */
    @PostConstruct
    public void init() {
        redisScriptComponent.preLoadLuaScript(SCRIPT_TRY_SET_SMS_CODE, TRY_SET_SMS_CODE_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_VALIDATE_AND_CONSUME_SMS_CODE, VALIDATE_AND_CONSUME_SMS_CODE_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_DELETE_IF_MATCH_SMS_CODE, DELETE_IF_MATCH_SMS_CODE_LUA);
    }

    /**
     * 尝试写入验证码（原子限频）。
     *
     * @param mobile 手机号，不能为空
     * @param code 验证码，不能为空
     * @param currentTimeMillis 当前毫秒时间戳
     * @param resendIntervalMillis 重发间隔（毫秒）
     * @param ttlSeconds 验证码有效期（秒）
     * @return true 表示写入成功；false 表示触发限频
     */
    public boolean trySetCode(String mobile, String code, long currentTimeMillis, long resendIntervalMillis, long ttlSeconds) {
        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(code)) {
            return false;
        }
        String smsCodeKey = smsCodeRedisKeyBuilder.buildSmsCodeKey(mobile);
        String codeValue = buildCodeValue(code, currentTimeMillis);
        Long result = redisScriptComponent.executeLuaScript(
                SCRIPT_TRY_SET_SMS_CODE,
                ReturnType.INTEGER,
                Collections.singletonList(smsCodeKey),
                Arrays.asList(
                        String.valueOf(currentTimeMillis),
                        String.valueOf(resendIntervalMillis),
                        String.valueOf(ttlSeconds),
                        codeValue
                )
        );
        return Objects.equals(result, 1L);
    }

    /**
     * 校验并消费验证码（一次性使用）。
     *
     * @param mobile 手机号，不能为空
     * @param verifyCode 用户输入验证码，不能为空
     * @return true 表示校验成功且已删除验证码；false 表示验证码不存在或不匹配
     */
    public boolean validateAndConsumeCode(String mobile, String verifyCode) {
        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(verifyCode)) {
            return false;
        }
        String smsCodeKey = smsCodeRedisKeyBuilder.buildSmsCodeKey(mobile);
        Long result = redisScriptComponent.executeLuaScript(
                SCRIPT_VALIDATE_AND_CONSUME_SMS_CODE,
                ReturnType.INTEGER,
                Collections.singletonList(smsCodeKey),
                Collections.singletonList(verifyCode)
        );
        return Objects.equals(result, 1L);
    }

    /**
     * 按值比对删除验证码。
     *
     * @param mobile 手机号，不能为空
     * @param expectedValue 预期验证码完整值（code_timestamp）
     * @return true 表示删除成功；false 表示当前值不一致或 key 不存在
     */
    public boolean deleteCodeIfMatch(String mobile, String expectedValue) {
        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(expectedValue)) {
            return false;
        }
        String smsCodeKey = smsCodeRedisKeyBuilder.buildSmsCodeKey(mobile);
        Long result = redisScriptComponent.executeLuaScript(
                SCRIPT_DELETE_IF_MATCH_SMS_CODE,
                ReturnType.INTEGER,
                Collections.singletonList(smsCodeKey),
                Collections.singletonList(expectedValue)
        );
        return Objects.equals(result, 1L);
    }

    /**
     * 构建验证码存储值，格式为 "code_timestamp"。
     *
     * @param code 验证码
     * @param currentTimeMillis 生成时间（毫秒）
     * @return Redis 存储值
     */
    public String buildCodeValue(String code, long currentTimeMillis) {
        return code + StrUtil.UNDERLINE + currentTimeMillis;
    }
}
