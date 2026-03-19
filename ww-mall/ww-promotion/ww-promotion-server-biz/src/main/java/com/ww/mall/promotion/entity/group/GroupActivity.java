package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 16:50
 * @description: 拼团活动模版
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_activity")
@CompoundIndex(name = "idx_group_activity_spu_status", def = "{'spuId': 1, 'status': 1, 'enabled': 1}")
public class GroupActivity extends BaseDoc {

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 商品SPU ID
     */
    private Long spuId;

    /**
     * 兼容字段：默认SKU ID。
     * <p>
     * 新模型下拼团按 SPU 维度分享，支持同一团内购买不同 SKU，
     * 实际可售 SKU 由 {@link #skuRules} 决定。
     */
    private Long skuId;

    /**
     * 兼容字段：默认拼团价格。
     * <p>
     * 新模型下展示价取 skuRules 中最小拼团价。
     */
    private BigDecimal groupPrice;

    /**
     * 兼容字段：默认原价。
     */
    private BigDecimal originalPrice;

    /**
     * SKU 规则列表。
     */
    private List<GroupSkuRule> skuRules;

    /**
     * 拼团人数要求
     */
    private Integer requiredSize;

    /**
     * 拼团有效期（小时）
     */
    private Integer expireHours;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 活动状态：0-未开始，1-进行中，2-已结束，3-已取消
     */
    private Integer status;

    /**
     * 每人限购数量
     */
    private Integer limitPerUser;

    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer enabled;

    /**
     * 活动图片
     */
    private String imageUrl;

    /**
     * 排序权重
     */
    private Integer sortWeight;

    /**
     * SKU 规则。
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
         * 是否启用：1-启用，0-禁用。
         */
        private Integer enabled;
    }

    /**
     * 构建根据ID查询
     */
    public static Query buildIdQuery(String id) {
        return BaseDoc.buildIdQuery(id);
    }

    /**
     * 构建查询进行中的活动
     */
    public static Query buildActiveQuery(Integer status, Date now) {
        return new Query().addCriteria(
                Criteria.where("status").is(status)
                        .and("startTime").lte(now)
                        .and("endTime").gte(now)
        );
    }

    /**
     * 构建根据状态查询
     */
    public static Query buildStatusQuery(Integer status) {
        return new Query().addCriteria(Criteria.where("status").is(status));
    }

    /**
     * 构建根据SPU ID和状态查询
     */
    public static Query buildSpuIdAndStatusQuery(Long spuId, Integer status) {
        return new Query().addCriteria(
                Criteria.where("spuId").is(spuId)
                        .and("status").is(status)
                        .and("enabled").is(1)
        );
    }

    /**
     * 构建状态更新
     */
    public static Update buildStatusUpdate(Integer status) {
        return new Update().set("status", status);
    }

    /**
     * 构建启用状态更新
     */
    public static Update buildEnabledUpdate(Integer enabled) {
        return new Update().set("enabled", enabled);
    }

}
