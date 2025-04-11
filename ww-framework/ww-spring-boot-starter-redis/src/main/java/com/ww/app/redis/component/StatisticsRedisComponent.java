package com.ww.app.redis.component;

import cn.hutool.core.date.DatePattern;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.component.key.StatisticsRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-12-21 11:04
 * @description: 用于统计PV、UV等数据 采用HyperLogLog实现UV统计，String计数器实现PV统计
 */
@Slf4j
@Component
public class StatisticsRedisComponent {

    private static final long DEFAULT_EXPIRE_DAYS = 90;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.PURE_DATE_PATTERN);
    
    /**
     * 批量记录PV的Lua脚本
     */
    private static final String BATCH_RECORD_PV_SCRIPT = 
            "local keys = KEYS " +
            "local expire = ARGV[1] " +
            "local results = {} " +
            "for i = 1, #keys do " +
            "  local count = redis.call('INCR', keys[i]) " +
            "  redis.call('EXPIRE', keys[i], expire) " +
            "  table.insert(results, count) " +
            "end " +
            "return results";
    
    /**
     * 批量记录UV的Lua脚本
     */
    private static final String BATCH_RECORD_UV_SCRIPT = 
            "local keys = KEYS " +
            "local users = ARGV " +
            "local expire = ARGV[#ARGV] " +
            "local results = {} " +
            "for i = 1, #keys do " +
            "  local added = redis.call('PFADD', keys[i], users[i]) " +
            "  if added == 1 then " +
            "    redis.call('EXPIRE', keys[i], expire) " +
            "  end " +
            "  local count = redis.call('PFCOUNT', keys[i]) " +
            "  table.insert(results, count) " +
            "end " +
            "return results";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private StatisticsRedisKeyBuilder statisticsRedisKeyBuilder;
    
    private RedisScript<List<Long>> batchRecordPVScript;
    private RedisScript<List<Long>> batchRecordUVScript;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        // 使用原始方式初始化脚本，避免类型推断问题
        DefaultRedisScript<List<Long>> pvScript = new DefaultRedisScript<>();
        pvScript.setScriptText(BATCH_RECORD_PV_SCRIPT);
        pvScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
        this.batchRecordPVScript = pvScript;
        
        DefaultRedisScript<List<Long>> uvScript = new DefaultRedisScript<>();
        uvScript.setScriptText(BATCH_RECORD_UV_SCRIPT);
        uvScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
        this.batchRecordUVScript = uvScript;
    }

    /**
     * 记录PV
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 当前PV值
     */
    public Long recordPV(String bizType, String bizId) {
        return recordPV(bizType, bizId, LocalDate.now());
    }

    /**
     * 记录PV
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @param date    统计日期
     * @return 当前PV值
     */
    public Long recordPV(String bizType, String bizId, LocalDate date) {
        if (StringUtils.isAnyBlank(bizType, bizId) || date == null) {
            throw new ApiException("参数不能为空");
        }
        try {
            String key = statisticsRedisKeyBuilder.buildPVKey(bizType, bizId, date.format(DATE_FORMATTER));
            Long count = redisTemplate.opsForValue().increment(key);
            // 设置过期时间
            redisTemplate.expire(key, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            return count;
        } catch (Exception e) {
            log.error("记录PV异常: bizType={}, bizId={}, date={}", bizType, bizId, date, e);
            throw new ApiException("记录PV异常: " + e.getMessage());
        }
    }
    
    /**
     * 批量记录PV
     *
     * @param bizType 业务类型
     * @param bizIds  业务ID列表
     * @return 各业务ID的当前PV值
     */
    public Map<String, Long> batchRecordPV(String bizType, List<String> bizIds) {
        return batchRecordPV(bizType, bizIds, LocalDate.now());
    }
    
    /**
     * 批量记录PV
     *
     * @param bizType 业务类型
     * @param bizIds  业务ID列表
     * @param date    统计日期
     * @return 各业务ID的当前PV值
     */
    public Map<String, Long> batchRecordPV(String bizType, List<String> bizIds, LocalDate date) {
        if (StringUtils.isBlank(bizType) || bizIds == null || bizIds.isEmpty() || date == null) {
            throw new ApiException("参数不能为空");
        }
        try {
            List<String> keys = new ArrayList<>(bizIds.size());
            for (String bizId : bizIds) {
                keys.add(statisticsRedisKeyBuilder.buildPVKey(bizType, bizId, date.format(DATE_FORMATTER)));
            }
            // 使用Lua脚本批量执行
            List<Long> results = redisTemplate.execute(batchRecordPVScript, keys, 
                    String.valueOf(DEFAULT_EXPIRE_DAYS * 24 * 60 * 60));
            // 构建结果Map
            Map<String, Long> resultMap = new HashMap<>(bizIds.size());
            for (int i = 0; i < bizIds.size(); i++) {
                resultMap.put(bizIds.get(i), results.get(i));
            }
            return resultMap;
        } catch (Exception e) {
            log.error("批量记录PV异常: bizType={}, bizIds={}, date={}", bizType, bizIds, date, e);
            throw new ApiException("批量记录PV异常: " + e.getMessage());
        }
    }

    /**
     * 记录UV
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @param userId  用户ID
     * @return 当前UV值
     */
    public Long recordUV(String bizType, String bizId, String userId) {
        return recordUV(bizType, bizId, userId, LocalDate.now());
    }

    /**
     * 记录UV
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @param userId  用户ID
     * @param date    统计日期
     * @return 当前UV值
     */
    public Long recordUV(String bizType, String bizId, String userId, LocalDate date) {
        if (StringUtils.isAnyBlank(bizType, bizId, userId) || date == null) {
            throw new ApiException("参数不能为空");
        }
        try {
            String key = statisticsRedisKeyBuilder.buildUVKey(bizType, bizId, date.format(DATE_FORMATTER));
            boolean added = redisTemplate.opsForHyperLogLog().add(key, userId) > 0;
            // 设置过期时间
            if (added) {
                redisTemplate.expire(key, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            return redisTemplate.opsForHyperLogLog().size(key);
        } catch (Exception e) {
            log.error("记录UV异常: bizType={}, bizId={}, userId={}, date={}", bizType, bizId, userId, date, e);
            throw new ApiException("记录UV异常: " + e.getMessage());
        }
    }
    
    /**
     * 批量记录UV
     *
     * @param bizType 业务类型
     * @param bizIds  业务ID列表
     * @param userId  用户ID
     * @return 各业务ID的当前UV值
     */
    public Map<String, Long> batchRecordUV(String bizType, List<String> bizIds, String userId) {
        return batchRecordUV(bizType, bizIds, userId, LocalDate.now());
    }
    
    /**
     * 批量记录UV
     *
     * @param bizType 业务类型
     * @param bizIds  业务ID列表
     * @param userId  用户ID
     * @param date    统计日期
     * @return 各业务ID的当前UV值
     */
    public Map<String, Long> batchRecordUV(String bizType, List<String> bizIds, String userId, LocalDate date) {
        if (StringUtils.isAnyBlank(bizType, userId) || bizIds == null || bizIds.isEmpty() || date == null) {
            throw new ApiException("参数不能为空");
        }
        try {
            List<String> keys = new ArrayList<>(bizIds.size());
            List<String> users = new ArrayList<>(bizIds.size());
            for (String bizId : bizIds) {
                keys.add(statisticsRedisKeyBuilder.buildUVKey(bizType, bizId, date.format(DATE_FORMATTER)));
                users.add(userId);
            }
            // 添加过期时间参数
            users.add(String.valueOf(DEFAULT_EXPIRE_DAYS * 24 * 60 * 60));
            // 使用Lua脚本批量执行
            List<Long> results = redisTemplate.execute(batchRecordUVScript, keys, users.toArray());
            // 构建结果Map
            Map<String, Long> resultMap = new HashMap<>(bizIds.size());
            for (int i = 0; i < bizIds.size(); i++) {
                resultMap.put(bizIds.get(i), results.get(i));
            }
            return resultMap;
        } catch (Exception e) {
            log.error("批量记录UV异常: bizType={}, bizIds={}, userId={}, date={}", bizType, bizIds, userId, date, e);
            throw new ApiException("批量记录UV异常: " + e.getMessage());
        }
    }

    /**
     * 获取指定日期范围的PV统计数据
     *
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 日期-PV映射
     */
    public Map<String, Long> getPVStatistics(String bizType, String bizId, LocalDate fromDate, LocalDate toDate) {
        if (StringUtils.isAnyBlank(bizType, bizId) || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ApiException("参数不合法");
        }
        try {
            Map<String, Long> result = new HashMap<>();
            List<String> keys = new ArrayList<>();
            // 收集所有需要查询的key
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                keys.add(statisticsRedisKeyBuilder.buildPVKey(bizType, bizId, dateStr));
                result.put(dateStr, 0L);
                currentDate = currentDate.plusDays(1);
            }
            // 批量获取PV数据
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (int i = 0; i < keys.size(); i++) {
                    String value = values.get(i);
                    if (value != null) {
                        String dateStr = fromDate.plusDays(i).format(DATE_FORMATTER);
                        result.put(dateStr, Long.parseLong(value));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取PV统计数据异常: bizType={}, bizId={}, fromDate={}, toDate={}", 
                    bizType, bizId, fromDate, toDate, e);
            throw new ApiException("获取PV统计数据异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取业务类型的PV统计数据
     *
     * @param bizType  业务类型
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 日期-PV映射
     */
    public Map<String, Long> getBizTypePVStatistics(String bizType, LocalDate fromDate, LocalDate toDate) {
        if (StringUtils.isBlank(bizType) || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ApiException("参数不合法");
        }
        try {
            Map<String, Long> result = new HashMap<>();
            List<String> keys = new ArrayList<>();
            // 收集所有需要查询的key
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                keys.add(statisticsRedisKeyBuilder.buildBizTypePVKey(bizType, dateStr));
                result.put(dateStr, 0L);
                currentDate = currentDate.plusDays(1);
            }
            // 批量获取PV数据
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (int i = 0; i < keys.size(); i++) {
                    String value = values.get(i);
                    if (value != null) {
                        String dateStr = fromDate.plusDays(i).format(DATE_FORMATTER);
                        result.put(dateStr, Long.parseLong(value));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取业务类型PV统计数据异常: bizType={}, fromDate={}, toDate={}", bizType, fromDate, toDate, e);
            throw new ApiException("获取业务类型PV统计数据异常: " + e.getMessage());
        }
    }

    /**
     * 获取指定日期范围的UV统计数据
     *
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 日期-UV映射
     */
    public Map<String, Long> getUVStatistics(String bizType, String bizId, LocalDate fromDate, LocalDate toDate) {
        if (StringUtils.isAnyBlank(bizType, bizId) || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ApiException("参数不合法");
        }
        try {
            Map<String, Long> result = new HashMap<>();
            // 获取每天的UV数据
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                String key = statisticsRedisKeyBuilder.buildUVKey(bizType, bizId, dateStr);
                Long count = redisTemplate.opsForHyperLogLog().size(key);
                result.put(dateStr, count);
                currentDate = currentDate.plusDays(1);
            }
            return result;
        } catch (Exception e) {
            log.error("获取UV统计数据异常: bizType={}, bizId={}, fromDate={}, toDate={}", bizType, bizId, fromDate, toDate, e);
            throw new ApiException("获取UV统计数据异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取业务类型的UV统计数据
     *
     * @param bizType  业务类型
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 日期-UV映射
     */
    public Map<String, Long> getBizTypeUVStatistics(String bizType, LocalDate fromDate, LocalDate toDate) {
        if (StringUtils.isBlank(bizType) || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ApiException("参数不合法");
        }
        try {
            Map<String, Long> result = new HashMap<>();
            // 获取每天的UV数据
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                String key = statisticsRedisKeyBuilder.buildBizTypeUVKey(bizType, dateStr);
                Long count = redisTemplate.opsForHyperLogLog().size(key);
                result.put(dateStr, count);
                currentDate = currentDate.plusDays(1);
            }
            return result;
        } catch (Exception e) {
            log.error("获取业务类型UV统计数据异常: bizType={}, fromDate={}, toDate={}", bizType, fromDate, toDate, e);
            throw new ApiException("获取业务类型UV统计数据异常: " + e.getMessage());
        }
    }

    /**
     * 获取指定日期范围的UV去重统计
     *
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 时间范围内的UV去重总数
     */
    public Long getUVMergeCount(String bizType, String bizId, LocalDate fromDate, LocalDate toDate) {
        if (StringUtils.isAnyBlank(bizType, bizId) || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ApiException("参数不合法");
        }
        try {
            List<String> keys = new ArrayList<>();
            // 收集所有需要合并的key
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                keys.add(statisticsRedisKeyBuilder.buildUVKey(bizType, bizId, dateStr));
                currentDate = currentDate.plusDays(1);
            }
            // 使用HyperLogLog的merge操作计算去重UV
            String mergeKey = statisticsRedisKeyBuilder.buildUVMergeKey(bizType, bizId, 
                    fromDate.format(DATE_FORMATTER), toDate.format(DATE_FORMATTER));
            redisTemplate.opsForHyperLogLog().union(mergeKey, 
                    keys.toArray(new String[0]));
            // 设置临时merge key的过期时间
            redisTemplate.expire(mergeKey, 1, TimeUnit.HOURS);
            return redisTemplate.opsForHyperLogLog().size(mergeKey);
        } catch (Exception e) {
            log.error("获取UV去重统计异常: bizType={}, bizId={}, fromDate={}, toDate={}", bizType, bizId, fromDate, toDate, e);
            throw new ApiException("获取UV去重统计异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取业务类型的UV去重统计
     *
     * @param bizType  业务类型
     * @param fromDate 开始日期
     * @param toDate   结束日期
     * @return 时间范围内的UV去重总数
     */
    public Long getBizTypeUVMergeCount(String bizType, LocalDate fromDate, LocalDate toDate) {
        if (StringUtils.isBlank(bizType) || fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ApiException("参数不合法");
        }
        try {
            List<String> keys = new ArrayList<>();
            // 收集所有需要合并的key
            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                keys.add(statisticsRedisKeyBuilder.buildBizTypeUVKey(bizType, dateStr));
                currentDate = currentDate.plusDays(1);
            }
            // 使用HyperLogLog的merge操作计算去重UV
            String mergeKey = statisticsRedisKeyBuilder.buildBizTypeUVMergeKey(bizType, 
                    fromDate.format(DATE_FORMATTER), toDate.format(DATE_FORMATTER));
            redisTemplate.opsForHyperLogLog().union(mergeKey, 
                    keys.toArray(new String[0]));
            // 设置临时merge key的过期时间
            redisTemplate.expire(mergeKey, 1, TimeUnit.HOURS);
            return redisTemplate.opsForHyperLogLog().size(mergeKey);
        } catch (Exception e) {
            log.error("获取业务类型UV去重统计异常: bizType={}, fromDate={}, toDate={}", bizType, fromDate, toDate, e);
            throw new ApiException("获取业务类型UV去重统计异常: " + e.getMessage());
        }
    }
    
    /**
     * 记录业务类型PV
     *
     * @param bizType 业务类型
     * @return 当前PV值
     */
    public Long recordBizTypePV(String bizType) {
        return recordBizTypePV(bizType, LocalDate.now());
    }
    
    /**
     * 记录业务类型PV
     *
     * @param bizType 业务类型
     * @param date    统计日期
     * @return 当前PV值
     */
    public Long recordBizTypePV(String bizType, LocalDate date) {
        if (StringUtils.isBlank(bizType) || date == null) {
            throw new ApiException("参数不能为空");
        }
        try {
            String key = statisticsRedisKeyBuilder.buildBizTypePVKey(bizType, date.format(DATE_FORMATTER));
            Long count = redisTemplate.opsForValue().increment(key);
            // 设置过期时间
            redisTemplate.expire(key, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            return count;
        } catch (Exception e) {
            log.error("记录业务类型PV异常: bizType={}, date={}", bizType, date, e);
            throw new ApiException("记录业务类型PV异常: " + e.getMessage());
        }
    }
    
    /**
     * 记录业务类型UV
     *
     * @param bizType 业务类型
     * @param userId  用户ID
     * @return 当前UV值
     */
    public Long recordBizTypeUV(String bizType, String userId) {
        return recordBizTypeUV(bizType, userId, LocalDate.now());
    }
    
    /**
     * 记录业务类型UV
     *
     * @param bizType 业务类型
     * @param userId  用户ID
     * @param date    统计日期
     * @return 当前UV值
     */
    public Long recordBizTypeUV(String bizType, String userId, LocalDate date) {
        if (StringUtils.isAnyBlank(bizType, userId) || date == null) {
            throw new ApiException("参数不能为空");
        }
        try {
            String key = statisticsRedisKeyBuilder.buildBizTypeUVKey(bizType, date.format(DATE_FORMATTER));
            boolean added = redisTemplate.opsForHyperLogLog().add(key, userId) > 0;
            // 设置过期时间
            if (added) {
                redisTemplate.expire(key, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            return redisTemplate.opsForHyperLogLog().size(key);
        } catch (Exception e) {
            log.error("记录业务类型UV异常: bizType={}, userId={}, date={}", bizType, userId, date, e);
            throw new ApiException("记录业务类型UV异常: " + e.getMessage());
        }
    }
} 