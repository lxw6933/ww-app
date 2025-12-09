package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2025-12-08 16:50
 * @description: 拼团活动模版
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_activity")
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
     * 商品SKU ID
     */
    private Long skuId;

    /**
     * 拼团价格
     */
    private BigDecimal groupPrice;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

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
     * 库存总数
     */
    private Integer totalStock;

    /**
     * 已售数量
     */
    private Integer soldCount;

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

    /**
     * 构建已售数量更新
     */
    public static Update buildSoldCountUpdate(Integer soldCount) {
        return new Update().set("soldCount", soldCount);
    }

}
