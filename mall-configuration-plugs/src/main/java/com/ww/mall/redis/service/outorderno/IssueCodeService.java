package com.ww.mall.redis.service.outorderno;

import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author ww
 * @create 2024-08-27- 09:15
 * @description:
 */
@Slf4j
@Component
@DependsOn("mongoTemplate")
public class IssueCodeService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String REDIS_SCRIPT_SHA1_KEY = "script:sha1:";

    private static final String OUT_ORDER_CODE_BLOOM_FILTER = "bf:outOrderCode";
    private static final String OUT_ORDER_CODE_SET = "set:outOrderCode";
    private static final String CONVERT_CODE_LIST = "list:convertCodes:";

    // list code 数量阈值
    private static final int CODE_NUM_THRESHOLD = 100;

    private static final String issueScriptName = "issueCodes";
    private static final String issueScript = "local redeemCodeList = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "local available = redis.call('LLEN', redeemCodeList)\n" +
            "local codes = {}\n" +
            "if available < quantity then\n" +
            "    return {}\n" +
            "end\n" +
            "return redis.call('LRANGE', redeemCodeList, 0, quantity - 1)\n" +
            "redis.call('LTRIM', redeemCodeList, quantity, -1)";

    private String issueScriptSha1;

    @Autowired
    private RedissonClient redissonClient;

    private RBloomFilter<String> bloomFilter;

    private static final int SHARD_NUM = 10000;

    private static final int DEFAULT_INIT_SIZE = 100000000;

    private static final double DEFAULT_FALSE_PROBABILITY = 0.01;

    // 批量入库的数量阈值
    private static final int BATCH_SIZE = 1000;

    private static final int CODE_RESULT_THREAD_POOL_SIZE = 10;
    private static final ConcurrentLinkedQueue<IssueCodeRecord> recordQueue = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService codeResultScheduler = Executors.newScheduledThreadPool(1);
    private static final ExecutorService codeResultExecutor = Executors.newFixedThreadPool(CODE_RESULT_THREAD_POOL_SIZE);

    public void addRecordToQueue(IssueCodeRecord issueCodeRecord) {
        recordQueue.offer(issueCodeRecord);
        log.info("【入队】outOrderCode【{}】codes【{}】", issueCodeRecord.getOutOrderCode(), issueCodeRecord.getCodes());
    }

    public void batchSaveIssueResult() {
        while (!recordQueue.isEmpty()) {
            // 检查队列元素是否突发流量
            this.checkQueueOverFlow();

            List<IssueCodeRecord> targetList = new ArrayList<>(BATCH_SIZE);
            for (int i = 0; i < BATCH_SIZE; i++) {
                IssueCodeRecord issueCodeRecord = recordQueue.poll();
                if (issueCodeRecord != null) {
                    log.info("【出队】outOrderCode【{}】codes【{}】", issueCodeRecord.getOutOrderCode(), issueCodeRecord.getCodes());
                    targetList.add(issueCodeRecord);
                } else {
                    break;
                }
            }
            if (!targetList.isEmpty()) {
                try {
                    mongoTemplate.insert(targetList, IssueCodeRecord.class);
                    log.info("【批量入库 数量: {}】", targetList.size());
                } catch (Exception e) {
                    log.error("【批量入库异常】", e);
                    targetList.forEach(errorRecord -> log.error("【批量入库异常】outOrderCode【{}】codes【{}】", errorRecord.getOutOrderCode(), errorRecord.getCodes()));
                }
            }
        }
    }

    public void checkQueueOverFlow() {
        int queueSize = recordQueue.size();
        if (queueSize > CODE_RESULT_THREAD_POOL_SIZE * BATCH_SIZE) {
            log.info("【本地队列溢出，总数量：{}】", queueSize);
            int batchesToProcess = queueSize / BATCH_SIZE;
            for (int i = 0; i < batchesToProcess; i++) {
                codeResultExecutor.submit(this::batchSaveIssueResult);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        int queueSize = recordQueue.size();
        log.info("【服务关闭，处理队列剩余发放结果，剩余数量：{}】", queueSize);
        int batchesToProcess = queueSize / BATCH_SIZE;
        for (int i = 0; i < batchesToProcess; i++) {
            codeResultExecutor.submit(this::batchSaveIssueResult);
        }
        codeResultExecutor.shutdown();
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
        // 从 Redis 中获取已存在的 SHA1
        String sha1Key = REDIS_SCRIPT_SHA1_KEY + scriptName;
        Object scriptSha1 = redissonClient.getBucket(sha1Key).get();

        if (scriptSha1 != null && !scriptSha1.toString().isEmpty()) {
            // 验证 SHA1 是否依然有效
            List<Boolean> existingScripts = scriptExecutor.scriptExists(scriptSha1.toString());
            if (existingScripts.get(0)) {
                // 如果有效，直接使用这个 SHA1
                return scriptSha1.toString();
            }
        }
        // 如果 SHA1 无效或不存在，重新加载脚本
        scriptSha1 = scriptExecutor.scriptLoad(script);
        redissonClient.getBucket(sha1Key).set(scriptSha1);
        return scriptSha1.toString();
    }

    @PostConstruct
    public void init() {
        // 开启定时任务批量处理发放结果
        codeResultScheduler.scheduleAtFixedRate(this::batchSaveIssueResult, 0, 1, TimeUnit.MINUTES);
        // 初始化外部单号过滤器
        bloomFilter = redissonClient.getBloomFilter(OUT_ORDER_CODE_BLOOM_FILTER);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(DEFAULT_INIT_SIZE, DEFAULT_FALSE_PROBABILITY);
        }
        // 预加载脚本
        issueScriptSha1 = preLoadScript(issueScriptName, issueScript);
    }

    /**
     * 添加外部单号到过滤器
     *
     * @param outOrderCode 外部单号
     */
    private void add(String outOrderCode) {
        bloomFilter.add(outOrderCode);
        log.info("【{}】to BloomFilter", outOrderCode);
    }

    /**
     * 获取set分区key
     *
     * @param outOrderCode 外部单号
     * @return set分区key
     */
    private String getShardKey(String outOrderCode) {
        int shardId = Math.abs(outOrderCode.hashCode()) % SHARD_NUM;
        return OUT_ORDER_CODE_SET + shardId;
    }

    /**
     * 外部订单号是否重复
     *
     * @param outOrderCode 外部订单号
     * @return boolean
     */
    private boolean checkOutOrderCode(String outOrderCode) {
        if (StringUtils.isEmpty(outOrderCode)) {
            throw new IllegalArgumentException("外部订单号不能为空");
        }
        if (outOrderCode.length() > 64) {
            throw new IllegalArgumentException("外部订单号长度不能超过64");
        }
        try {
            // get outOrderCode set shard key
            String shardKey = this.getShardKey(outOrderCode);
            RSet<String> outOrderCodeSet = redissonClient.getSet(shardKey);
            if (!bloomFilter.contains(outOrderCode)) {
                // not exists bloomFilter add to bloomFilter
                this.add(outOrderCode);
            }
            // check in Set and add if not exists
            return !outOrderCodeSet.add(outOrderCode);
        } catch (Exception e) {
            log.error("【{}】校验异常", outOrderCode, e);
            throw e;
        }
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
        log.info("【{}】发放数量【{}】活动【{}】", outOrderCode, quantity, actCode);
        if (this.checkOutOrderCode(outOrderCode)) {
            log.info("【{}】重复使用", outOrderCode);
            // return before codes result
            throw new ApiException("当前单号已发放过兑换码，请勿重复使用");
        }

        RScript scriptExecutor = redissonClient.getScript();
        List<Object> keys = Collections.singletonList(CONVERT_CODE_LIST + actCode);
        List<String> result = scriptExecutor.evalSha(RScript.Mode.READ_WRITE, issueScriptSha1, RScript.ReturnType.MULTI, keys, quantity);
        log.info("【{}】发放结果：{}", outOrderCode, result);
        // result valid
        if (result.isEmpty()) {
            // TODO 异步通知服务补充兑换码数量
            throw new ApiException("兑换码数量不足，请稍后再试");
        } else {
            IssueCodeRecord issueCodeRecord = new IssueCodeRecord();
            issueCodeRecord.setOutOrderCode(outOrderCode);
            issueCodeRecord.setCodes(result);
            addRecordToQueue(issueCodeRecord);
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

}












