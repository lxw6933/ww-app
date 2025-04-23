package com.ww.app.redis.controller;

import com.ww.app.common.utils.ShortCodeUtil;
import com.ww.app.redis.component.ShortCodeRedisComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 短码生成器示例控制器
 *
 * @author ww
 */
@Slf4j
@RestController
@RequestMapping("/short-code")
public class ShortCodeDemoController {

    @Resource
    private ShortCodeRedisComponent shortCodeRedisComponent;

    /**
     * 生成一个本地短码
     *
     * @param length 短码长度
     * @return 生成的短码
     */
    @GetMapping("/local")
    public String generateLocalShortCode(@RequestParam(value = "length", defaultValue = "5") int length) {
        return ShortCodeUtil.nextShortCode(length);
    }

    /**
     * 生成一个分布式短码（基于Redis）
     *
     * @param businessType 业务类型
     * @param length       短码长度
     * @return 生成的短码
     */
    @GetMapping("/distributed")
    public String generateDistributedShortCode(
            @RequestParam(value = "businessType", defaultValue = "test") String businessType,
            @RequestParam(value = "length", defaultValue = "5") int length) {
        return shortCodeRedisComponent.nextShortCode(businessType, length);
    }

    /**
     * 生成一个业务短码（带前缀）
     *
     * @param businessType 业务类型
     * @param length       短码长度
     * @return 生成的短码
     */
    @GetMapping("/business")
    public String generateBusinessShortCode(
            @RequestParam(value = "businessType", defaultValue = "test") String businessType,
            @RequestParam(value = "length", defaultValue = "5") int length) {
        String shortCode = shortCodeRedisComponent.nextShortCode(businessType, length);
        return businessType.toUpperCase() + shortCode;
    }

    /**
     * 批量生成短码
     *
     * @param businessType 业务类型
     * @param count        数量
     * @param length       短码长度
     * @return 生成的短码数组
     */
    @GetMapping("/batch")
    public String[] batchGenerateShortCodes(
            @RequestParam(value = "businessType", defaultValue = "test") String businessType,
            @RequestParam(value = "count", defaultValue = "10") int count,
            @RequestParam(value = "length", defaultValue = "5") int length) {
        return shortCodeRedisComponent.batchNextShortCodes(businessType, count, length);
    }

    /**
     * 重置短码生成器
     *
     * @param businessType 业务类型
     * @param length       短码长度
     * @return 操作结果
     */
    @GetMapping("/reset")
    public String resetGenerator(
            @RequestParam(value = "businessType", defaultValue = "test") String businessType,
            @RequestParam(value = "length", defaultValue = "5") int length) {
        shortCodeRedisComponent.reset(businessType, length);
        return "短码生成器已重置: 业务类型=" + businessType + ", 长度=" + length;
    }

    /**
     * 性能测试
     *
     * @param count  生成数量
     * @param length 短码长度
     * @return 测试结果
     */
    @GetMapping("/performance")
    public Map<String, Object> performanceTest(
            @RequestParam(value = "count", defaultValue = "1000") int count,
            @RequestParam(value = "length", defaultValue = "5") int length) {
        Map<String, Object> result = new HashMap<>(8);

        // 本地模式性能测试
        long localStart = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            ShortCodeUtil.nextShortCode(length);
        }
        long localEnd = System.currentTimeMillis();
        long localCost = localEnd - localStart;
        double localTps = count * 1000.0 / Math.max(1, localCost);

        // 分布式模式性能测试
        long distStart = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            shortCodeRedisComponent.nextShortCode("perf_test", length);
        }
        long distEnd = System.currentTimeMillis();
        long distCost = distEnd - distStart;
        double distTps = count * 1000.0 / Math.max(1, distCost);

        // 结果
        result.put("count", count);
        result.put("length", length);
        result.put("localCostMs", localCost);
        result.put("localTps", String.format("%.2f", localTps));
        result.put("distCostMs", distCost);
        result.put("distTps", String.format("%.2f", distTps));

        log.info("性能测试结果: 数量={}, 长度={}, 本地耗时={}ms, 本地TPS={}, 分布式耗时={}ms, 分布式TPS={}",
                count, length, localCost, String.format("%.2f", localTps), distCost, String.format("%.2f", distTps));

        return result;
    }
} 