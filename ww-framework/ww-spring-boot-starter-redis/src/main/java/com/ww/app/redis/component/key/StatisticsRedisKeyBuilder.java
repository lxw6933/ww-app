package com.ww.app.redis.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-21 10:38
 * @description: 统计数据Redis Key构建器 用于生成PV、UV等统计数据的Redis key
 */
@Component
public class StatisticsRedisKeyBuilder extends RedisKeyBuilder {

    /**
     * PV统计key前缀
     */
    public static final String PV_PREFIX = "pv";
    
    /**
     * UV统计key前缀
     */
    public static final String UV_PREFIX = "uv";
    
    /**
     * UV合并统计key前缀
     */
    public static final String UV_MERGE_PREFIX = "uv:merge";

    /**
     * 构建PV统计key
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @param date    日期字符串(格式：yyyyMMdd)
     * @return Redis key
     */
    public String buildPVKey(String bizType, String bizId, String date) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, PV_PREFIX, bizType, bizId, date);
    }

    /**
     * 构建UV统计key
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @param date    日期字符串(格式：yyyyMMdd)
     * @return Redis key
     */
    public String buildUVKey(String bizType, String bizId, String date) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, UV_PREFIX, bizType, bizId, date);
    }

    /**
     * 构建UV合并统计key
     *
     * @param bizType  业务类型
     * @param bizId    业务ID
     * @param fromDate 开始日期字符串(格式：yyyyMMdd)
     * @param toDate   结束日期字符串(格式：yyyyMMdd)
     * @return Redis key
     */
    public String buildUVMergeKey(String bizType, String bizId, String fromDate, String toDate) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, UV_MERGE_PREFIX, bizType, bizId, fromDate, toDate);
    }
    
    /**
     * 构建业务类型PV统计key
     *
     * @param bizType 业务类型
     * @param date    日期字符串(格式：yyyyMMdd)
     * @return Redis key
     */
    public String buildBizTypePVKey(String bizType, String date) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, PV_PREFIX, bizType, date);
    }
    
    /**
     * 构建业务类型UV统计key
     *
     * @param bizType 业务类型
     * @param date    日期字符串(格式：yyyyMMdd)
     * @return Redis key
     */
    public String buildBizTypeUVKey(String bizType, String date) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, UV_PREFIX, bizType, date);
    }
    
    /**
     * 构建业务类型UV合并统计key
     *
     * @param bizType  业务类型
     * @param fromDate 开始日期字符串(格式：yyyyMMdd)
     * @param toDate   结束日期字符串(格式：yyyyMMdd)
     * @return Redis key
     */
    public String buildBizTypeUVMergeKey(String bizType, String fromDate, String toDate) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, UV_MERGE_PREFIX, bizType, fromDate, toDate);
    }
} 