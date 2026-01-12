package com.ww.app.member.job;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.interfaces.BulkDataHandler;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.common.utils.date.DateTimeValidator;
import com.ww.app.member.component.SignComponent;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import com.ww.app.member.entity.mongo.MemberSignRecord;
import com.ww.app.member.enums.SignPeriod;
import com.ww.app.member.strategy.sign.AbstractSignStrategy;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.listener.KeyScanListener;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.ww.app.common.utils.date.DateTimeValidator.WEEKLY_PATTERN;
import static com.ww.app.redis.key.RedisKeyBuilder.SPLIT_ITEM;

/**
 * @author ww
 * @create 2025-10-15 16:20
 * @description: 签到数据持久化任务
 */
@Slf4j
@Component
public class SignJob {

    private static final int BATCH_SIZE = 500;

    @Resource
    private AppRedisTemplate appRedisTemplate;

    @Resource
    private SignRedisKeyBuilder signRedisKeyBuilder;

    @Resource
    private SignComponent signComponent;

    @Resource
    private BulkDataHandler<MemberSignRecord> mongoBulkDataHandler;

    private static final ExecutorService executorService = ThreadUtil.initFixedThreadPoolExecutor("sign-job", 10);

    public void signDataBatchHandle(String lastPeriodKey, Collection<String> signDataKeyList, SignPeriod signPeriod) {
        executorService.execute(() -> {
            List<MemberSignRecord> batchList = new ArrayList<>(BATCH_SIZE);
            List<String> persistKeyList = new ArrayList<>(BATCH_SIZE);
            try {
                signDataKeyList.forEach(key -> {
                    MemberSignRecord record = buildRecordFromRedis(key, lastPeriodKey, signPeriod);
                    if (record != null) {
                        batchList.add(record);
                        persistKeyList.add(key);
                    }
                });
                try {
                    if (batchList.isEmpty()) {
                        return;
                    }
                    int success = mongoBulkDataHandler.bulkSave(batchList);
                    log.info("批量落库 {} 条记录, 成功插入 {} 条记录", batchList.size(), success);
                    if (success == batchList.size()) {
                        appRedisTemplate.batchRemoveKeys(new ArrayList<>(persistKeyList), true);
                    }
                } catch (Exception e) {
                    log.error("批量落库失败", e);
                }
            } catch (Exception e) {
                log.error("归档出错", e);
            }
        });
    }

    @XxlJob("archiveMonthlySignDataJobHandler")
    public void archiveMonthlySignDataJobHandler(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            // 上个月，如 202509
            dateStr = getPreviousMonthPeriodKey();
        } else {
            Assert.isTrue(DateTimeValidator.isValidYearMonth(dateStr), () -> new IllegalArgumentException("输入月时间格式异常"));
        }
        final String lastPeriodKey = dateStr;
        archiveSignData(lastPeriodKey, SignPeriod.MONTHLY);
    }

    @XxlJob("archiveWeeklySignDataJobHandler")
    public void archiveWeeklySignDataJobHandler(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            // 上周，如 2025W1
            dateStr = getPreviousWeekPeriodKey();
        } else {
            Assert.isTrue(DateTimeValidator.isValidYearWeek(dateStr), () -> new IllegalArgumentException("输入周时间格式异常"));
        }
        final String lastPeriodKey = dateStr;
        archiveSignData(lastPeriodKey, SignPeriod.WEEKLY);
    }

    private void archiveSignData(String lastPeriodKey, SignPeriod signPeriod) {
        log.info("开始归档签到数据 periodKey={} signType={}", lastPeriodKey, signPeriod);
        String pattern;
        switch (signPeriod) {
            case WEEKLY:
                pattern = signRedisKeyBuilder.buildWeeklySignPatternKey(lastPeriodKey);
                break;
            case MONTHLY:
                pattern = signRedisKeyBuilder.buildMonthlySignPatternKey(lastPeriodKey);
                break;
            default:
                return;
        }

        List<String> signDataKeyList = new ArrayList<>();
        appRedisTemplate.scanKeys(pattern, new KeyScanListener() {
                    @Override
                    public void onKey(String key) {
                        signDataKeyList.add(key);
                        if (signDataKeyList.size() >= BATCH_SIZE) {
                            signDataBatchHandle(lastPeriodKey, new ArrayList<>(signDataKeyList), signPeriod);
                            signDataKeyList.clear();
                        }
                    }

                    @Override
                    public void onFinish() {
                        log.info("pattern:[{}] key 扫描完毕", pattern);
                        if (!signDataKeyList.isEmpty()) {
                            log.info("签到数据处理最后一批归档数据数量：[{}]", signDataKeyList.size());
                            signDataBatchHandle(lastPeriodKey, new ArrayList<>(signDataKeyList), signPeriod);
                            signDataKeyList.clear();
                        }
                    }
                }
        );
        log.info("签到数据归档完成，periodKey={}", lastPeriodKey);
    }

    private String getPreviousMonthPeriodKey() {
        LocalDate now = LocalDate.now().minusMonths(1);
        return now.format(DateTimeFormatter.ofPattern(DatePattern.SIMPLE_MONTH_PATTERN));
    }

    private String getPreviousWeekPeriodKey() {
        LocalDate now = LocalDate.now().minusWeeks(1);
        return now.format(DateTimeFormatter.ofPattern(WEEKLY_PATTERN));
    }

    private MemberSignRecord buildRecordFromRedis(String key, String periodKey, SignPeriod signPeriod) {
        try {
            AbstractSignStrategy strategy = signComponent.getStrategy(signPeriod);
            // 获取周期内最后一天
            LocalDate periodDate = strategy.getEndDate(periodKey);

            // 解析redis key 获取用户id
            Long userId = Long.valueOf(key.split(SPLIT_ITEM)[2]);
            byte[] bitmap = strategy.getSignBytes(key);
            if (bitmap == null) return null;

            // 获取签到数据Hex
            String bitmapHex = MemberSignRecord.encodeBitmap(bitmap);

            // 获取当月签到总次数
            int totalSignDays = strategy.getBitmapSignCount(key);

            // 获取连续签到次数
            // 获取位数
            int bits = strategy.getBitCount(periodDate);
            // 获取当前位置
            int position = strategy.getOffset(periodDate);
            int currentStreak = strategy.getBitmapStreakSignCount(key, bits, position);

            // 获取补签次数
            int retroSignCount = strategy.getResignCount(userId, periodDate);

            return MemberSignRecord.builder().build()
                    .setMemberId(userId)
                    .setSignPeriod(signPeriod)
                    .setPeriodKey(periodKey)
                    .setBitmap(bitmapHex)
                    .setTotalSignDays(totalSignDays)
                    .setCurrentStreakSignDays(currentStreak)
                    .setRetroSignDays(retroSignCount);
        } catch (Exception e) {
            log.error("解析用户签到记录出错 key={}", key, e);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        ThreadUtil.shutdown("signJobExecutor", () -> {}, executorService);
    }

}
