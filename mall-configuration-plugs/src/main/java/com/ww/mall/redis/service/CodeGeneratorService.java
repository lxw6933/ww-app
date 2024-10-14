package com.ww.mall.redis.service;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ww
 * @create 2024-10-10- 16:36
 * @description:
 */
@Slf4j
@Component
public class CodeGeneratorService {

    @Autowired
    private CodeBloomFilterComponent codeBloomFilterComponent;

    public final AtomicBoolean running = new AtomicBoolean(false);

    public static final int BATCH_NUM = 1000;
    public static final int CODE_GENERATOR_THREAD_POOL_SIZE = 20;
    public static final int MAX_NUM = BATCH_NUM * CODE_GENERATOR_THREAD_POOL_SIZE;

    public static final ExecutorService codeGeneratorExecutor = Executors.newFixedThreadPool(CODE_GENERATOR_THREAD_POOL_SIZE);

    public int doGeneratorCode(String batchNo, int length, int totalCount) {
        Assert.isTrue(this.running.compareAndSet(false, true), () -> new ApiException("正在生成code"));
        try {
            return generatorCodes(batchNo, length, totalCount);
        } finally {
            this.running.set(false);
        }
    }


    /**
     * 生成code
     *
     * @param batchNo 批次号
     * @param length code长度
     * @param totalCount 生成总数量
     * @return 成功数量
     */
    private int generatorCodes(String batchNo, int length, int totalCount) {
        Assert.isFalse(totalCount > MAX_NUM, () -> new ApiException("一次性最多生成" + MAX_NUM + "兑换码数量"));
        int taskCount = totalCount / BATCH_NUM;
        CountDownLatch doneSignal = new CountDownLatch(taskCount + 1);

        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < taskCount + 1; i++) {
            int start = i * BATCH_NUM;
            int end = Math.min(start + BATCH_NUM, totalCount);
            int num = end - start;
            // 提交任务到线程池
            Future<Integer> result = codeGeneratorExecutor.submit(() -> {
                try {
                    return processCodeTask(batchNo, results.size(), length, num);
                } finally {
                    doneSignal.countDown();
                }
            });
            results.add(result);
        }
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            log.error("生成code中断异常", e);
            throw new RuntimeException(e);
        }

        // 处理所有任务的结果
        int totalSuccess = 0;
        for (Future<Integer> result : results) {
            // 累加成功处理的数量
            int successCount = 0;
            try {
                successCount = result.get();
            } catch (Exception e) {
                log.error("批次号【{}】生成兑换码异常", batchNo, e);
            }
            totalSuccess += successCount;
        }
        log.info("批次号【{}】所有任务完成，总共成功处理的数量：{}", batchNo, totalSuccess);
        return totalSuccess;
    }

    private Integer processCodeTask(String batchNo, int taskNo, int length, int num) {
        try {
            Set<String> codes = batchGenerateCodes(length, num);
            // TODO 数据处理 入库
            log.info("任务【{}】批次号【{}】成功生成【{}】个code", taskNo, batchNo, codes.size());
            return codes.size();
        } catch (Exception e) {
            log.error("任务【{}】批次号【{}】codes生成异常", taskNo, batchNo, e);
            return 0;
        }
    }

    /**
     * 生成批次兑换码集合
     *
     * @param length 兑换码长度
     * @param count 兑换码数量
     * @return 批次兑换码集合
     */
    private Set<String> batchGenerateCodes(int length, int count) {
        Set<String> codes = new HashSet<>();
        while (codes.size() < count) {
            String code = RandomUtil.randomString(length);
            boolean result = codeBloomFilterComponent.addData(code);
            if (result) {
                codes.add(code);
            }
        }
        return codes;
    }

}
