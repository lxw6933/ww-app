package com.ww.mall.redis.service.outorderno;

import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2024-08-27- 09:15
 * @description:
 */
@Slf4j
@Component
public class IssueCodeService {

    private static final String OUT_ORDER_CODE_BLOOM_FILTER = "bf:outOrderCode";
    private static final String OUT_ORDER_CODE_SET = "set:outOrderCode";
    private static final String CONVERT_CODE_LIST = "list:convertCodes";

    private static final int CODE_NUM_THRESHOLD = 100;
    private static final String CODE_NOT_ENOUGH = "CODE_NOT_ENOUGH";
    private static final String SUCCESS = "SUCCESS";

    private String issueScriptSha1;

    @Autowired
    private RedissonClient redissonClient;

    private RBloomFilter<String> bloomFilter;

    private static final int SHARD_NUM = 10000;

    private static final int DEFAULT_INIT_SIZE = 100000000;

    private static final double DEFAULT_FALSE_PROBABILITY = 0.01;

    public void preLoadScript() {
        String issueScript = "local redeemCodeList = KEYS[1]\n" +
                "local quantity = tonumber(ARGV[1])\n" +
                "local codes = {}\n" +
                "local available = redis.call('LLEN', redeemCodeList)\n" +
                "if available < quantity then\n" +
                "    return { 'ERROR_NOT_ENOUGH_CODES' }\n" +
                "end\n" +
                "for i = 1, quantity do\n" +
                "    local code = redis.call('LPOP', redeemCodeList)\n" +
                "    if code then\n" +
                "        table.insert(codes, code)\n" +
                "    else\n" +
                "        break\n" +
                "    end\n" +
                "end\n" +
                "local remaining = redis.call('LLEN', redeemCodeList)\n" +
                "return { 'SUCCESS', unpack(codes), remaining }";
        RScript scriptExecutor = redissonClient.getScript();
        issueScriptSha1 = scriptExecutor.scriptLoad(issueScript);
    }

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter(OUT_ORDER_CODE_BLOOM_FILTER);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(DEFAULT_INIT_SIZE, DEFAULT_FALSE_PROBABILITY);
            log.info("{} BloomFilter initialized with expected insertions: {} and false positive rate: {}", OUT_ORDER_CODE_BLOOM_FILTER, DEFAULT_INIT_SIZE, DEFAULT_FALSE_PROBABILITY);
        } else {
            log.info("{} BloomFilter already exists. Skipping initialization", OUT_ORDER_CODE_BLOOM_FILTER);
        }
        // 预加载脚本
        preLoadScript();
    }

    public void add(String value) {
        bloomFilter.add(value);
        log.info("added value: {} to BloomFilter {}", value, OUT_ORDER_CODE_BLOOM_FILTER);
    }

    public boolean mightContain(String value) {
        boolean result = bloomFilter.contains(value);
        log.info("checked value: {} in BloomFilter {}, result: {}", value, OUT_ORDER_CODE_BLOOM_FILTER, result);
        return result;
    }

    public void deleteBloomFilter() {
        bloomFilter.delete();
        log.info("{} BloomFilter deleted", OUT_ORDER_CODE_BLOOM_FILTER);
    }

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
    public boolean checkOutOrderCode(String outOrderCode) {
        if (StringUtils.isEmpty(outOrderCode)) {
            return false;
        }
        if (outOrderCode.length() > 64) {
            throw new IllegalArgumentException("外部订单号长度不能超过64");
        }
        try {
            // get outOrderCode set shard key
            String shardKey = this.getShardKey(outOrderCode);
            RSet<String> outOrderCodeSet = redissonClient.getSet(shardKey);
            if (!this.mightContain(outOrderCode)) {
                // not exists bloomFilter add to bloomFilter
                this.add(outOrderCode);
            }
            // check in Set and add if not exists
            return !outOrderCodeSet.add(outOrderCode);
        } catch (Exception e) {
            log.error("outOrderCode {} check exception", outOrderCode, e);
            throw e;
        }
    }

    /**
     * 发放兑换码
     *
     * @param outOrderCode 外部订单号
     * @param quantity 需要发放的兑换码数量
     * @return List<String> 已发放的兑换码列表
     */
    public RedeemCodeResult distributeCodes(String outOrderCode, int quantity) {
        if (this.checkOutOrderCode(outOrderCode)) {
            log.info("outOrderCode {} has already been processed", outOrderCode);
            // return before codes result
            throw new ApiException("当前单号已发放过兑换码，请勿重复使用");
        }

        RScript scriptExecutor = redissonClient.getScript();
        List<Object> keys = Collections.singletonList(CONVERT_CODE_LIST);
        List<Object> args = Collections.singletonList(String.valueOf(quantity));
        List<String> result = scriptExecutor.evalSha(RScript.Mode.READ_WRITE, issueScriptSha1, RScript.ReturnType.MULTI, keys, args);
        log.info("outOrderCode【{}】issue result：{}", outOrderCode, result);
        // result valid
        if (result == null || result.size() < 2) {
            throw new RuntimeException("issue script result: " + result);
        }

        String status = result.get(0);
        if (CODE_NOT_ENOUGH.equals(status)) {
            throw new ApiException("兑换码数量不足，请稍后再试");
        }
        if (!SUCCESS.equals(status)) {
            throw new ApiException("兑换码发放失败，请稍后再试");
        }

        List<String> redeemCodes = new ArrayList<>();
        for (int i = 1; i < result.size() - 1; i++) {
            redeemCodes.add(result.get(i));
        }
        int remaining = (int) Long.parseLong(result.get(result.size() - 1));
        if (remaining < CODE_NUM_THRESHOLD) {
            // TODO 通知兑换码服务补充兑换码
        }
        log.info("distributed {} codes【{}】for outOrderCode {}.", quantity, redeemCodes, outOrderCode);
        return new RedeemCodeResult(status, redeemCodes, remaining);
    }

}
