package com.ww.app.member.job;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.interfaces.BulkDataHandler;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.member.component.SignComponent;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import com.ww.app.member.entity.mongo.MemberSignRecord;
import com.ww.app.member.enums.SignPeriodEnum;
import com.ww.app.member.enums.SignType;
import com.ww.app.member.strategy.sign.SignStrategyFactory;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.listener.KeyScanListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import static com.ww.app.member.component.key.SignRedisKeyBuilder.WEEK_FLAG;
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
    private BulkDataHandler<MemberSignRecord> MongoBulkDataHandler;

    @Resource
    private SignStrategyFactory signStrategyFactory;

    private static final ExecutorService executorService = ThreadUtil.initFixedThreadPoolExecutor("sign-job", 10);

    public void signDataBatchHandle(String lastPeriodKey, Collection<String> signDataKeyList, SignType signType) {
        executorService.execute(() -> {
            List<MemberSignRecord> batchList = new ArrayList<>(BATCH_SIZE);
            try {
                signDataKeyList.forEach(key -> {
                    MemberSignRecord record = buildRecordFromRedis(key, lastPeriodKey, signType);
                    batchList.add(record);
                });
                try {
                    int success = MongoBulkDataHandler.bulkSave(batchList);
                    log.info("批量落库 {} 条记录, 成功插入 {} 条记录", batchList.size(), success);
                } catch (Exception e) {
                    log.error("批量落库失败", e);
                }
            } catch (Exception e) {
                log.error("归档出错", e);
            }
        });
    }

//    @XxlJob("archiveMonthSignDataJobHandler")
    public void archiveMonthSignDataJobHandler(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            // 上个月，如 202509
            dateStr = getLastPeriodKey();
        }
        final String lastPeriodKey = dateStr;
        log.info("开始归档签到数据 periodKey={} signType={}", lastPeriodKey, SignType.MONTH);

        String pattern = signRedisKeyBuilder.buildMonthlySignPatternKey(lastPeriodKey);

        List<String> signDataKeyList = new ArrayList<>();
        appRedisTemplate.scanKeys(pattern, new KeyScanListener() {
                    @Override
                    public void onKey(String key) {
                        signDataKeyList.add(key);
                        if (signDataKeyList.size() >= BATCH_SIZE) {
                            signDataBatchHandle(lastPeriodKey, new ArrayList<>(signDataKeyList), SignType.MONTH);
                            signDataKeyList.clear();
                        }
                    }

                    @Override
                    public void onFinish() {
                        log.info("pattern:[{}] key 扫描完毕", pattern);
                        if (!signDataKeyList.isEmpty()) {
                            log.info("签到数据处理最后一批归档数据数量：[{}]", signDataKeyList.size());
                            signDataBatchHandle(lastPeriodKey, new ArrayList<>(signDataKeyList), SignType.MONTH);
                            signDataKeyList.clear();
                        }
                    }
                }
        );

        log.info("签到数据归档完成，periodKey={}", lastPeriodKey);
    }

    private String getLastPeriodKey() {
        LocalDate now = LocalDate.now().minusMonths(1);
        return now.format(DateTimeFormatter.ofPattern(DatePattern.SIMPLE_MONTH_PATTERN));
    }

    private MemberSignRecord buildRecordFromRedis(String key, String periodKey, SignType signType) {
        try {
            LocalDate periodDate = null;
            switch (signType) {
                case WEEK:
                    // 返回该月的最后一天
                    periodDate = getMonthEndDate(periodKey);
                    break;
                case MONTH:
                    // 返回该周最后一天
                    periodDate = getMonthEndDate(periodKey);
                    break;
                default:
            }

            Long userId = Long.valueOf(key.split(SPLIT_ITEM)[2]);
            byte[] bitmap = signComponent.getSignBytes(key);
            if (bitmap == null) return null;

            // 获取签到数据Hex
            String bitmapHex = Hex.encodeHexString(bitmap);

            // 获取当月签到总次数
            int totalSignDays = signComponent.getSignCount(key);

            SignPeriodEnum period = null;
            switch (signType) {
                case WEEK:
                    period = SignPeriodEnum.WEEKLY;
                    break;
                case MONTH:
                    period = SignPeriodEnum.MONTHLY;
                    break;
                default:
            }

            // 获取连续签到次数
            // 获取位数
            int bits = signStrategyFactory.getStrategy(period).getBitCount(periodDate);
            // 获取当前位置
            int position = signStrategyFactory.getStrategy(period).getOffset(periodDate);
            int currentStreak = signComponent.getStreakSignCount(key, bits, position);

            // 获取补签次数
            String countKey = signRedisKeyBuilder.buildResignCountPrefixKey(userId, periodDate);
            int retroSignCount = signComponent.getResignCount(countKey);

            return MemberSignRecord.builder().build()
                    .setMemberId(userId)
                    .setSignType(signType)
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

    public LocalDate getWeekEndDate(String weekKey) {
        // 解析年份和周数
        String[] parts = weekKey.split(WEEK_FLAG);
        if (parts.length != 2) {
            throw new IllegalArgumentException("周数格式错误，应为: yyyyWww");
        }

        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);

        // 获取该周的第一天（周一）
        LocalDate firstDayOfWeek = LocalDate.of(year, 1, 1)
                .with(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(), week)
                .with(DayOfWeek.MONDAY);

        // 获取该周的最后一天（周日）
        return firstDayOfWeek.with(DayOfWeek.SUNDAY);
    }

    public LocalDate getMonthEndDate(String monthKey) {
        YearMonth yearMonth = YearMonth.parse(monthKey, DateTimeFormatter.ofPattern(DatePattern.SIMPLE_MONTH_PATTERN));
        return yearMonth.atEndOfMonth();
    }

    @PreDestroy
    public void destroy() {
        ThreadUtil.shutdown("signJobExecutor", () -> {}, executorService);
    }

}
