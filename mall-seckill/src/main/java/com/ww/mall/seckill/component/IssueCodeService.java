package com.ww.mall.seckill.component;

import cn.hutool.core.date.DateUtil;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.mongodb.handler.MongoBulkDataHandler;
import com.ww.mall.redis.service.UniqueService;
import com.ww.mall.seckill.entity.IssueCodeRecord;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2024-08-27- 09:15
 * @description:
 */
@Slf4j
@Component
@ConditionalOnBean(MongoBulkDataHandler.class)
public class IssueCodeService {

    @Resource
    private MongoBulkDataHandler<IssueCodeRecord> mongoBulkDataHandler;

    @Resource
    private RedissonClient redissonClient;

    private UniqueService uniqueService;

    private static RecordQueueComponent codeCurrentQueueComponent;

    private static final String REDIS_SCRIPT_SHA1_KEY = "script:sha1:";

    private static final String OUT_ORDER_CODE_KEY = "outOrderCode";
    private static final String CONVERT_CODE_LIST = "list:convertCodes:";

    private static final String issueScriptName = "issueCodes";
    private static final String issueScript = "local redeemCodeList = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "local available = redis.call('LLEN', redeemCodeList)\n" +
            "local codes = {}\n" +
            "if available < quantity then\n" +
            "    return {}\n" +
            "end\n" +
            "codes = redis.call('LRANGE', redeemCodeList, 0, quantity - 1)\n" +
            "redis.call('LTRIM', redeemCodeList, quantity, -1)\n" +
            "return codes";

    private String issueScriptSha1;

    /**
     * 入队
     *
     * @param issueCodeRecord 发放结果记录
     */
    public void addRecordToQueue(IssueCodeRecord issueCodeRecord) {
        codeCurrentQueueComponent.addRecordToQueue(issueCodeRecord);
    }

    /**
     * 预加载lua脚本【解决集群重复预加载】
     *
     * @param scriptName 脚本名称
     * @param script     脚本
     * @return 脚本sha1
     */
    private String preLoadScript(String scriptName, String script) {
        RScript scriptExecutor = redissonClient.getScript();
        String sha1Key = REDIS_SCRIPT_SHA1_KEY + scriptName;
        Object scriptSha1 = redissonClient.getBucket(sha1Key).get();

        if (scriptSha1 != null && !scriptSha1.toString().isEmpty()) {
            List<Boolean> existingScripts = scriptExecutor.scriptExists(scriptSha1.toString());
            if (existingScripts.get(0)) {
                return scriptSha1.toString();
            }
        }
        scriptSha1 = scriptExecutor.scriptLoad(script);
        redissonClient.getBucket(sha1Key).set(scriptSha1);
        return scriptSha1.toString();
    }

    @PostConstruct
    public void init() {
        // init outOrderCode uniqueService
        uniqueService = new UniqueService(redissonClient, OUT_ORDER_CODE_KEY);
        // init code result queueComponent
        codeCurrentQueueComponent = new RecordQueueComponent(mongoBulkDataHandler);
        // preload lua script
        issueScriptSha1 = preLoadScript(issueScriptName, issueScript);
    }

    /**
     * 发放兑换码
     *
     * @param actCode      活动编码
     * @param outOrderCode 外部订单号
     * @param quantity     需要发放的兑换码数量
     * @return List<String> 已发放的兑换码列表
     */
    public List<String> distributeCodes(String actCode, String outOrderCode, int quantity) {
        if (uniqueService.checkOutOrderCode(outOrderCode)) {
            log.warn("[{}]重复使用", outOrderCode);
            // return before codes result
            throw new ApiException("当前单号已发放过兑换码，请勿重复使用");
        }

        RScript scriptExecutor = redissonClient.getScript();
        List<Object> keys = Collections.singletonList(CONVERT_CODE_LIST + actCode);
        List<String> result = scriptExecutor.evalSha(RScript.Mode.READ_WRITE, issueScriptSha1, RScript.ReturnType.MULTI, keys, quantity);
        log.info("[{}]发放结果：{}", outOrderCode, result);
        // result valid
        if (result.isEmpty()) {
            if (!uniqueService.removeTargetFormSet(outOrderCode)) {
                log.warn("外部单号[{}]移除失败", outOrderCode);
            }
            // TODO 异步通知服务补充兑换码数量
            throw new ApiException("兑换码数量不足，请稍后再试");
        } else {
            addRecordToQueue(new IssueCodeRecord(outOrderCode, result, DateUtil.formatDateTime(new Date())));
            return result;
        }
    }

    /**
     * 补充兑换码
     *
     * @param actCode  活动编码
     * @param newCodes 新码集合
     * @return 数量
     */
    public boolean addRedeemCodes(String actCode, List<String> newCodes) {
        RList<Object> list = redissonClient.getList(CONVERT_CODE_LIST + actCode);
        return list.addAll(newCodes);
    }

    @PreDestroy
    public void destroy() {
        codeCurrentQueueComponent.destroy();
    }

}












