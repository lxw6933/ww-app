package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.promotion.enums.GroupActivityStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 拼团活动实体。
 * <p>
 * 当前设计下，活动状态不再落库保存，而是基于开始时间、结束时间实时推导。
 * 这样可以避免定时任务延迟、任务丢失或多节点时钟差异导致的状态漂移。
 *
 * @author ww
 * @create 2025-12-08 16:50
 * @description: 拼团活动模板
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_activity")
@CompoundIndexes({
        @CompoundIndex(name = "idx_group_activity_spu_enabled_time", def = "{'spuId': 1, 'enabled': 1, 'startTime': 1, 'endTime': 1}"),
        @CompoundIndex(name = "idx_group_activity_enabled_time", def = "{'enabled': 1, 'startTime': 1, 'endTime': 1}")
})
public class GroupActivity extends BaseDoc {

    /**
     * 活动名称。
     */
    private String name;

    /**
     * 活动描述。
     */
    private String description;

    /**
     * 商品 SPU ID。
     */
    private Long spuId;

    /**
     * 兼容字段：默认展示 SKU ID。
     * <p>
     * 新模型按 SPU 维度共享拼团，实际可售 SKU 由 {@link #skuRules} 决定。
     */
    private Long skuId;

    /**
     * 兼容字段：默认展示拼团价。
     * <p>
     * 新模型下会从启用的 SKU 规则中挑选展示价最低的一条写入该字段，
     * 方便旧调用方仍然按单 SKU 模型读取展示数据。
     */
    private BigDecimal groupPrice;

    /**
     * 兼容字段：默认展示原价。
     */
    private BigDecimal originalPrice;

    /**
     * SKU 规则列表。
     */
    private List<GroupSkuRule> skuRules;

    /**
     * 成团人数要求。
     */
    private Integer requiredSize;

    /**
     * 开团后有效期，单位小时。
     */
    private Integer expireHours;

    /**
     * 活动开始时间。
     */
    private Date startTime;

    /**
     * 活动结束时间。
     */
    private Date endTime;

    /**
     * 每人限购数量。
     */
    private Integer limitPerUser;

    /**
     * 是否启用，1-启用，0-禁用。
     */
    private Integer enabled;

    /**
     * 活动图片。
     */
    private String imageUrl;

    /**
     * 排序权重。
     */
    private Integer sortWeight;

    /**
     * 活动累计开团数。
     * <p>
     * 活动进行中优先由 Redis 维护实时值；活动结束后会将最终值归档落库，
     * 便于历史查询、报表统计和后续清理 Redis 运行态统计 Key。
     * 仅在“首次开团成功”时累计加一，不包含幂等回放。
     */
    private Long openGroupCount;

    /**
     * 活动累计参团人数。
     * <p>
     * 活动进行中优先由 Redis 维护实时值；活动结束后会将最终值归档落库。
     * 当前口径按累计参团人次统计，团长开团也计入一次。
     */
    private Long joinMemberCount;

    /**
     * 活动统计是否已归档。
     * <p>
     * 该标记用于保证“活动结束后统计落库并删除 Redis Key”动作具备幂等性，
     * 避免多实例任务并发执行时重复归档或重复删 Key。
     */
    private Boolean statsSettled;

    /**
     * 活动统计归档时间。
     */
    private Date statsSettledTime;

    /**
     * 运行时动态计算活动状态。
     * <p>
     * 该值不落库，只用于接口返回和内存内业务判断。
     * 规则：
     * 1. 当前时间早于开始时间，返回未开始。
     * 2. 当前时间晚于结束时间，返回已结束。
     * 3. 其他情况返回进行中。
     *
     * @return 动态状态码
     */
    @Transient
    public Integer getStatus() {
        return resolveStatus(new Date());
    }

    /**
     * 根据指定时间推导活动状态。
     * <p>
     * 该方法显式接收时间参数，便于单元测试、批量判断和不同上下文下复用。
     *
     * @param now 参考时间，为空时按当前系统时间处理
     * @return 动态状态码
     */
    public Integer resolveStatus(Date now) {
        Date currentTime = now == null ? new Date() : now;
        if (startTime != null && startTime.after(currentTime)) {
            return GroupActivityStatus.NOT_STARTED.getCode();
        }
        if (endTime != null && !endTime.after(currentTime)) {
            return GroupActivityStatus.ENDED.getCode();
        }
        return GroupActivityStatus.ACTIVE.getCode();
    }

    /**
     * 判断活动在指定时刻是否已经进入活动时间窗。
     * <p>
     * 该判断只关注开始和结束时间，不叠加 enabled 条件，
     * 以便在“活动进行中禁止修改核心交易字段”这类规则中复用。
     *
     * @param now 参考时间
     * @return true-位于活动时间窗内
     */
    public boolean isActiveAt(Date now) {
        return GroupActivityStatus.ACTIVE.getCode() == resolveStatus(now);
    }

    /**
     * SKU 维度规则。
     */
    @Data
    public static class GroupSkuRule {

        /**
         * SKU ID。
         */
        private Long skuId;

        /**
         * SKU 拼团价。
         */
        private BigDecimal groupPrice;

        /**
         * SKU 原价。
         */
        private BigDecimal originalPrice;

        /**
         * SKU 规则是否启用，1-启用，0-禁用。
         */
        private Integer enabled;
    }

    /**
     * 根据主键构建查询条件。
     *
     * @param id 活动 ID
     * @return Mongo Query
     */
    public static Query buildIdQuery(String id) {
        return BaseDoc.buildIdQuery(id);
    }

    /**
     * 构建“当前可参与活动”查询。
     * <p>
     * 条件固定为：启用中、开始时间不晚于当前时间、结束时间晚于当前时间。
     *
     * @param now 查询参考时间
     * @return Mongo Query
     */
    public static Query buildActiveQuery(Date now) {
        return new Query().addCriteria(
                Criteria.where("enabled").is(1)
                        .and("startTime").lte(now)
                        .and("endTime").gt(now)
        );
    }

    /**
     * 构建指定 SPU 在当前时刻可参与的活动查询。
     *
     * @param spuId SPU ID
     * @param now 查询参考时间
     * @return Mongo Query
     */
    public static Query buildSpuIdAndActiveQuery(Long spuId, Date now) {
        return new Query().addCriteria(
                Criteria.where("spuId").is(spuId)
                        .and("enabled").is(1)
                        .and("startTime").lte(now)
                        .and("endTime").gt(now)
        );
    }

    /**
     * 构建“已结束但统计尚未归档”的活动查询。
     *
     * @param now 当前时间
     * @param limit 单次批量上限
     * @return Mongo Query
     */
    public static Query buildEndedUnsettledQuery(Date now, int limit) {
        Criteria unsettledCriteria = new Criteria().orOperator(
                Criteria.where("statsSettled").exists(false),
                Criteria.where("statsSettled").is(false)
        );
        Query query = new Query().addCriteria(
                Criteria.where("endTime").lte(now)
                        .andOperator(unsettledCriteria)
        );
        query.limit(limit);
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "endTime", "id"));
        return query;
    }

    /**
     * 构建按活动ID执行统计归档的幂等更新条件。
     *
     * @param activityId 活动ID
     * @return Mongo Query
     */
    public static Query buildIdAndUnsettledQuery(String activityId) {
        Criteria unsettledCriteria = new Criteria().orOperator(
                Criteria.where("statsSettled").exists(false),
                Criteria.where("statsSettled").is(false)
        );
        return new Query().addCriteria(
                Criteria.where("_id").is(activityId)
                        .andOperator(unsettledCriteria)
        );
    }

    /**
     * 构建活动统计归档更新内容。
     *
     * @param openGroupCount 累计开团数
     * @param joinMemberCount 累计参团人数
     * @param settledTime 归档时间
     * @return Mongo Update
     */
    public static Update buildStatisticsSettledUpdate(Long openGroupCount, Long joinMemberCount, Date settledTime) {
        return new Update()
                .set("openGroupCount", openGroupCount)
                .set("joinMemberCount", joinMemberCount)
                .set("statsSettled", true)
                .set("statsSettledTime", settledTime)
                .set("updateTime", settledTime);
    }

    /**
     * 构建启用状态更新。
     *
     * @param enabled 启用状态
     * @return Mongo Update
     */
    public static Update buildEnabledUpdate(Integer enabled) {
        return new Update().set("enabled", enabled);
    }
}
