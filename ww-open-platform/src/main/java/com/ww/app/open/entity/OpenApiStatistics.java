package com.ww.app.open.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 开放平台API统计实体（MongoDB 存储）
 */
@Data
@Document("open_api_statistics")
@EqualsAndHashCode(callSuper = true)
public class OpenApiStatistics extends BaseDoc {

    /** 统计日期（格式：yyyy-MM-dd） */
    private String statDate;

    /** 应用编码 */
    private String appCode;

    /** API编码 */
    private String apiCode;

    /** 总调用次数 */
    private Long totalCount;

    /** 成功调用次数 */
    private Long successCount;

    /** 失败调用次数 */
    private Long failCount;

    /** 平均响应时间（毫秒） - 读取时根据 totalDuration/totalCount 计算 */
    private Long avgDuration;

    /** 最大响应时间（毫秒） */
    private Long maxDuration;

    /** 最小响应时间（毫秒） */
    private Long minDuration;

    /** 累计总耗时（毫秒），用于计算平均值 */
    private Long totalDuration;

    /** 统计类型：0-按日，1-按小时 */
    private Integer statType;

    /** 统计小时（0-23），按日统计时为null */
    private Integer statHour;
}

