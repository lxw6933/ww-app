package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;

/**
 * @author ww
 * @create 2025-12-08 16:50
 * @description: 拼团成员记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_member")
@CompoundIndexes({
        @CompoundIndex(name = "idx_group_member_group_user", def = "{'groupInstanceId': 1, 'userId': 1, 'joinTime': 1}"),
        @CompoundIndex(name = "idx_group_member_group_join_time", def = "{'groupInstanceId': 1, 'joinTime': 1}"),
        @CompoundIndex(name = "idx_group_member_user_join_time", def = "{'userId': 1, 'joinTime': -1}"),
        @CompoundIndex(name = "idx_group_member_order", def = "{'orderId': 1}", unique = true)
})
public class GroupMember extends BaseDoc {

    /**
     * 拼团实例ID
     */
    private String groupInstanceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 商品SKU ID
     */
    private Long skuId;

    /**
     * 成员生命周期状态。
     */
    private String memberStatus;

    /**
     * 售后单号。
     */
    private String afterSaleId;

    /**
     * 构建根据订单ID查询
     */
    public static Query buildOrderIdQuery(String orderId) {
        return new Query().addCriteria(Criteria.where("orderId").is(orderId));
    }

    /**
     * 构建根据拼团实例ID查询所有成员
     */
    public static Query buildGroupInstanceIdQuery(String groupInstanceId) {
        return new Query().addCriteria(Criteria.where("groupInstanceId").is(groupInstanceId))
                .with(Sort.by(Sort.Direction.ASC, "joinTime", "id"));
    }

    /**
     * 构建根据用户ID查询参与团记录的条件。
     * <p>
     * “我的拼团”列表需要按最近参与时间倒序返回团摘要，
     * 因此直接命中用户维度索引并携带排序，避免应用层再做大规模重排。
     *
     * @param userId 用户ID
     * @return Mongo 查询条件
     */
    public static Query buildUserIdQuery(Long userId) {
        return new Query().addCriteria(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "joinTime", "id"));
    }

    /**
     * 构建基于订单号幂等 upsert 的更新内容。
     * <p>
     * 拼团成员轨迹是“按订单累积、按状态推进”的模型，不需要每次事件重建整团成员表。
     * 这里以 {@code orderId} 作为唯一业务键做增量 upsert，可显著降低 Mongo 删插放大。
     *
     * @param member 成员快照
     * @param now 当前时间，用于兜底填充审计字段
     * @return Mongo 更新对象
     */
    public static Update buildOrderIdUpsert(GroupMember member, Date now) {
        Update update = new Update();
        update.set("groupInstanceId", member.getGroupInstanceId());
        update.set("userId", member.getUserId());
        update.set("orderId", member.getOrderId());
        update.set("joinTime", member.getJoinTime());
        update.set("skuId", member.getSkuId());
        update.set("memberStatus", member.getMemberStatus());
        update.set("afterSaleId", member.getAfterSaleId());
        update.set("updateTime", member.getUpdateTime() != null ? member.getUpdateTime() : now);
        update.setOnInsert("createTime", member.getCreateTime() != null ? member.getCreateTime() : now);
        return update;
    }
}
