package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 拼团链路日志。
 * <p>
 * 该集合用于补齐拼团主流程中的数据闭环与排障闭环，记录请求入口、异步事件、
 * MQ 发送/消费、退款补偿等关键节点，便于在出现重复消费、异步落库失败、
 * MQ 投递异常时，按 traceId/groupId/orderId 进行串联排查。
 *
 * @author ww
 * @create 2026-03-16
 * @description: 拼团链路日志实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document("group_flow_log")
@CompoundIndex(name = "idx_group_id_create_time", def = "{'groupId': 1, 'createTime': 1}")
public class GroupFlowLog extends BaseDoc {

    /**
     * 链路追踪ID。
     */
    private String traceId;

    /**
     * 拼团实例ID。
     */
    private String groupId;

    /**
     * 活动ID。
     */
    private String activityId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 订单ID。
     */
    private String orderId;

    /**
     * 事件阶段，例如 CREATE_GROUP、SAVE_MEMBER、GROUP_SUCCESS_MQ。
     */
    private String eventType;

    /**
     * 日志来源，例如 GROUP_SERVICE、GROUP_PROCESSOR、GROUP_MQ_CONSUMER。
     */
    private String source;

    /**
     * 当前处理状态，例如 PROCESSING、SUCCESS、FAILED、SKIPPED。
     */
    private String status;

    /**
     * 重试次数。
     */
    private Integer retryCount;

    /**
     * 错误码。
     */
    private String errorCode;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 请求/事件快照，使用 JSON 字符串存储。
     */
    private String payloadSnapshot;

    /**
     * 构建按拼团ID查询并按发生时间正序排序的查询条件。
     *
     * @param groupId 拼团实例ID
     * @return Mongo 查询对象
     */
    public static Query buildGroupIdOrderByCreateTimeQuery(String groupId) {
        return new Query()
                .addCriteria(Criteria.where("groupId").is(groupId))
                .with(Sort.by(Sort.Direction.ASC, "createTime", "id"));
    }
}
