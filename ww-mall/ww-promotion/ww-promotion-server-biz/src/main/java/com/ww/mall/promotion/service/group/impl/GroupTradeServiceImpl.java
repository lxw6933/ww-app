package com.ww.mall.promotion.service.group.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.redis.component.RedissonComponent;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupTrade;
import com.ww.mall.promotion.enums.GroupFlowSource;
import com.ww.mall.promotion.enums.GroupFlowStage;
import com.ww.mall.promotion.enums.GroupFlowStatus;
import com.ww.mall.promotion.enums.GroupTradeStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.GroupTradeService;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;

/**
 * 拼团交易编排服务实现。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 负责承接支付成功和售后成功消息，编排正式拼团动作
 */
@Slf4j
@Service
public class GroupTradeServiceImpl implements GroupTradeService {

    @Resource
    private GroupInstanceService groupInstanceService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupFlowLogSupport groupFlowLogSupport;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Resource
    private RedissonComponent redissonComponent;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 处理支付成功消息。
     * <p>
     * 该入口只保留四件事：
     * 1. 校验消息和记录链路日志；
     * 2. 按订单维度加分布式锁，避免同一订单重复编排；
     * 3. 命中历史 SUCCESS/FAILED/PROCESSING 锚点时直接短路回放；
     * 4. 仅在首次处理时推进正式的开团/参团主流程。
     *
     * @param message 支付成功消息
     * @return 拼团结果
     */
    @Override
    public GroupInstanceVO handleOrderPaid(GroupOrderPaidMessage message) {
        validatePaidMessage(message);
        String traceId = hasText(message.getTraceId()) ? message.getTraceId() : groupFlowLogSupport.createTraceId();
        groupFlowLogSupport.record(traceId, message.getGroupId(), message.getActivityId(), message.getUserId(), message.getOrderId(),
                GroupFlowStage.PAY_ORDER_MQ, GroupFlowSource.GROUP_MQ_CONSUMER, GroupFlowStatus.PROCESSING,
                null, null, message);
        String lockKey = groupRedisKeyBuilder.buildTradeLockKey(message.getOrderId());
        RLock lock = redissonComponent.getRedissonClient().getLock(lockKey);
        try {
            // 同一个订单的支付成功回调只允许单线程进入编排，避免重复开团或重复参团。
            boolean locked = lock.tryLock(GroupBizConstants.GROUP_LOCK_WAIT_SECONDS,
                    GroupBizConstants.GROUP_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": 获取支付编排锁失败");
            }
            // 已经存在最终态锚点时优先回放，避免再次进入真正的业务编排。
            GroupInstanceVO completedResult = resolveCompletedTrade(findTrade(message.getOrderId()));
            if (completedResult != null) {
                return completedResult;
            }
            // 仅首次处理或未完成状态下，才执行正式的拼团动作。
            return executePaidTrade(traceId, message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": 锁等待被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 处理售后成功消息。
     *
     * @param message 售后成功消息
     */
    @Override
    public void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message) {
        if (message == null || !hasText(message.getOrderId())) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        groupInstanceService.handleAfterSaleSuccess(message);
    }

    /**
     * 执行支付成功后的主流程。
     * <p>
     * 这里统一收口三个动作：
     * 1. 先将交易锚点推进到 PROCESSING；
     * 2. 调用开团/参团服务完成真实业务动作；
     * 3. 成功则落 SUCCESS，失败则统一走失败锚点和补偿逻辑。
     *
     * @param traceId 链路追踪ID
     * @param message 支付成功消息
     * @return 拼团结果
     */
    private GroupInstanceVO executePaidTrade(String traceId, GroupOrderPaidMessage message) {
        try {
            // 先落 PROCESSING，便于重复回调命中时识别“另一条链路正在处理中”。
            saveTrade(traceId, message, null, GroupTradeStatus.PROCESSING, null);
            GroupInstanceVO result = doHandlePaid(message);
            // 真实业务完成后再推进 SUCCESS，避免中途成功标记污染幂等判断。
            saveTrade(traceId, message, result, GroupTradeStatus.SUCCESS, null);
            groupFlowLogSupport.record(traceId, result.getId(), result.getActivityId(), message.getUserId(), message.getOrderId(),
                    GroupFlowStage.PAY_ORDER_MQ, GroupFlowSource.GROUP_MQ_CONSUMER, GroupFlowStatus.SUCCESS,
                    null, null, message);
            return result;
        } catch (DuplicateKeyException e) {
            // 极端并发下 saveTrade 可能被唯一索引拦截，这里回放已落地结果。
            return resolveDuplicatedTrade(message);
        } catch (ApiException e) {
            // 业务异常保留原语义，同时统一记录 FAILED 和补偿信息。
            throw recordPaidFailure(traceId, message, e.getMessage(), e);
        } catch (Exception e) {
            // 非业务异常统一包装，避免底层实现细节直接暴露给上游。
            throw recordPaidFailure(traceId, message, e.getMessage(),
                    new ApiException(GROUP_RECORD_ERROR.getMsg() + ": " + e.getMessage()));
        }
    }

    /**
     * 执行正式拼团动作。
     *
     * @param message 支付成功消息
     * @return 拼团结果
     */
    private GroupInstanceVO doHandlePaid(GroupOrderPaidMessage message) {
        if (GroupTradeType.START == message.getTradeType()) {
            CreateGroupCommand createCommand = new CreateGroupCommand();
            createCommand.setActivityId(message.getActivityId());
            createCommand.setUserId(message.getUserId());
            createCommand.setOrderId(message.getOrderId());
            createCommand.setSkuId(message.getSkuId());
            createCommand.setOrderInfo(message.getOrderInfo());
            return groupInstanceService.createGroup(createCommand);
        }
        JoinGroupCommand joinCommand = new JoinGroupCommand();
        joinCommand.setGroupId(message.getGroupId());
        joinCommand.setUserId(message.getUserId());
        joinCommand.setOrderId(message.getOrderId());
        joinCommand.setSkuId(message.getSkuId());
        joinCommand.setOrderInfo(message.getOrderInfo());
        return groupInstanceService.joinGroup(joinCommand);
    }

    /**
     * 保存交易锚点。
     * <p>
     * 锚点是支付消息幂等和排障的核心凭证：
     * - SUCCESS 表示本订单已经完成拼团编排；
     * - FAILED 表示本订单已经失败并触发过补偿；
     * - PROCESSING 表示同一订单正在被其他线程处理。
     *
     * @param traceId 链路追踪ID
     * @param message 支付成功消息
     * @param result 拼团结果
     * @param status 交易状态
     * @param failReason 失败原因
     */
    private void saveTrade(String traceId, GroupOrderPaidMessage message, GroupInstanceVO result,
                           GroupTradeStatus status, String failReason) {
        GroupTrade trade = findTrade(message.getOrderId());
        Date now = new Date();
        if (trade == null) {
            trade = new GroupTrade();
            trade.setCreateTime(now);
        }
        trade.setTraceId(traceId);
        trade.setTradeType(message.getTradeType());
        trade.setStatus(status);
        trade.setActivityId(result != null ? result.getActivityId() : message.getActivityId());
        trade.setGroupId(result != null ? result.getId() : message.getGroupId());
        trade.setUserId(message.getUserId());
        trade.setOrderId(message.getOrderId());
        trade.setSpuId(result != null ? result.getSpuId() : message.getSpuId());
        trade.setSkuId(message.getSkuId());
        trade.setOrderInfo(message.getOrderInfo());
        trade.setCallbackTime(now);
        trade.setFailReason(failReason);
        trade.setUpdateTime(now);
        try {
            mongoTemplate.save(trade);
        } catch (DuplicateKeyException e) {
            // 只有确认还是同一笔 orderId 业务时才允许覆写，避免出现脏数据串单。
            GroupTrade existingTrade = findTrade(message.getOrderId());
            if (existingTrade == null || !isSameTrade(message, existingTrade)) {
                throw e;
            }
            existingTrade.setTraceId(traceId);
            existingTrade.setTradeType(message.getTradeType());
            existingTrade.setStatus(status);
            existingTrade.setActivityId(result != null ? result.getActivityId() : message.getActivityId());
            existingTrade.setGroupId(result != null ? result.getId() : message.getGroupId());
            existingTrade.setUserId(message.getUserId());
            existingTrade.setSpuId(result != null ? result.getSpuId() : message.getSpuId());
            existingTrade.setSkuId(message.getSkuId());
            existingTrade.setOrderInfo(message.getOrderInfo());
            existingTrade.setCallbackTime(now);
            existingTrade.setFailReason(failReason);
            existingTrade.setUpdateTime(now);
            mongoTemplate.save(existingTrade);
        }
    }

    /**
     * 根据订单ID查找交易锚点。
     *
     * @param orderId 订单ID
     * @return 交易锚点
     */
    private GroupTrade findTrade(String orderId) {
        return hasText(orderId) ? mongoTemplate.findOne(GroupTrade.buildOrderIdQuery(orderId), GroupTrade.class) : null;
    }

    /**
     * 回放已存在的交易锚点。
     * <p>
     * 该方法用于处理重复支付成功回调：
     * - SUCCESS 且有 groupId，直接返回最终团详情；
     * - FAILED，直接回放失败原因；
     * - PROCESSING，说明另一条链路还在执行，当前请求不再重复推进。
     *
     * @param trade 已存在的交易锚点
     * @return 已完成的拼团结果；若返回 null，表示需要继续执行主流程
     */
    private GroupInstanceVO resolveCompletedTrade(GroupTrade trade) {
        if (trade == null) {
            return null;
        }
        if (GroupTradeStatus.SUCCESS == trade.getStatus() && hasText(trade.getGroupId())) {
            return groupInstanceService.getGroupDetail(trade.getGroupId());
        }
        if (GroupTradeStatus.FAILED == trade.getStatus()) {
            throw new ApiException(trade.getFailReason() != null
                    ? trade.getFailReason() : GROUP_RECORD_ERROR.getMsg());
        }
        if (GroupTradeStatus.PROCESSING == trade.getStatus()) {
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": 当前订单正在处理中");
        }
        return null;
    }

    /**
     * 处理并发写入导致的唯一索引冲突。
     * <p>
     * 在多个支付成功回调同时落锚点时，当前线程可能因为唯一索引失败。
     * 此时不应直接报错，而应尝试把已落地的 SUCCESS/FAILED 结果回放给当前请求。
     *
     * @param message 支付成功消息
     * @return 已落地的拼团结果
     */
    private GroupInstanceVO resolveDuplicatedTrade(GroupOrderPaidMessage message) {
        GroupTrade duplicatedTrade = findTrade(message.getOrderId());
        if (duplicatedTrade != null && isSameTrade(message, duplicatedTrade)) {
            return resolveCompletedTrade(duplicatedTrade);
        }
        throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": 交易单存在并发写入，请稍后重试");
    }

    /**
     * 统一记录支付编排失败。
     * <p>
     * 所有失败分支都必须保持以下顺序一致：
     * 1. FAILED 落库；
     * 2. 发送单笔退款补偿消息；
     * 3. 记录失败链路日志。
     * 这样重复回调时，拼团域才能稳定回放失败态而不是再次执行补偿。
     *
     * @param traceId 链路追踪ID
     * @param message 支付成功消息
     * @param reason 失败原因
     * @param exception 最终抛给上游的异常
     * @return 传入的业务异常
     */
    private ApiException recordPaidFailure(String traceId, GroupOrderPaidMessage message, String reason, ApiException exception) {
        saveTrade(traceId, message, null, GroupTradeStatus.FAILED, reason);
        sendSingleRefund(traceId, message, reason);
        groupFlowLogSupport.recordFailure(traceId, message.getGroupId(), message.getActivityId(), message.getUserId(),
                message.getOrderId(), GroupFlowStage.PAY_ORDER_MQ, GroupFlowSource.GROUP_MQ_CONSUMER,
                reason, message);
        return exception;
    }

    /**
     * 发送单笔退款补偿消息。
     *
     * @param traceId 链路追踪ID
     * @param message 支付成功消息
     * @param reason 失败原因
     */
    private void sendSingleRefund(String traceId, GroupOrderPaidMessage message, String reason) {
        try {
            BigDecimal refundAmount = resolveRefundAmount(message);
            if (refundAmount == null) {
                log.error("发送拼团单笔退款消息失败，未能解析退款金额: orderId={}", message.getOrderId());
                return;
            }
            GroupRefundMessage.RefundOrder refundOrder = new GroupRefundMessage.RefundOrder();
            refundOrder.setUserId(message.getUserId());
            refundOrder.setOrderId(message.getOrderId());
            refundOrder.setRefundAmount(refundAmount);
            refundOrder.setIsLeader(GroupTradeType.START == message.getTradeType());
            GroupRefundMessage refundMessage = new GroupRefundMessage();
            refundMessage.setTraceId(traceId);
            refundMessage.setGroupId(message.getGroupId());
            refundMessage.setActivityId(message.getActivityId());
            refundMessage.setReason(reason);
            refundMessage.setRefundOrders(Collections.singletonList(refundOrder));
            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, refundMessage);
        } catch (Exception e) {
            log.error("发送拼团单笔退款消息失败: orderId={}", message.getOrderId(), e);
        }
    }

    /**
     * 解析退款金额。
     * <p>
     * 支付域优先透传标准 payAmount；若兼容回调未传，则从订单快照里做最佳努力解析，
     * 避免拼团失败后因为金额缺失而丢掉补偿动作。
     *
     * @param message 支付成功消息
     * @return 退款金额，无法识别时返回 null
     */
    private BigDecimal resolveRefundAmount(GroupOrderPaidMessage message) {
        if (message.getPayAmount() != null) {
            return message.getPayAmount();
        }
        if (!hasText(message.getOrderInfo())) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(message.getOrderInfo());
            BigDecimal refundAmount = readDecimal(root, "payAmount");
            if (refundAmount != null) {
                return refundAmount;
            }
            refundAmount = readDecimal(root, "amount");
            if (refundAmount != null) {
                return refundAmount;
            }
            refundAmount = readDecimal(root, "orderAmount");
            if (refundAmount != null) {
                return refundAmount;
            }
            return readDecimal(root, "actualAmount");
        } catch (Exception e) {
            log.warn("解析拼团退款金额失败: orderId={}", message.getOrderId(), e);
            return null;
        }
    }

    /**
     * 从 JSON 快照里读取金额字段。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @return 金额值，不存在或非法时返回 null
     */
    private BigDecimal readDecimal(JsonNode root, String fieldName) {
        if (root == null || !root.has(fieldName) || root.get(fieldName).isNull()) {
            return null;
        }
        JsonNode fieldNode = root.get(fieldName);
        if (fieldNode.isNumber()) {
            return fieldNode.decimalValue();
        }
        if (fieldNode.isTextual() && hasText(fieldNode.asText())) {
            try {
                return new BigDecimal(fieldNode.asText().trim());
            } catch (NumberFormatException e) {
                log.warn("拼团金额字段格式非法: fieldName={}, value={}", fieldName, fieldNode.asText());
            }
        }
        return null;
    }

    /**
     * 判断重复写入的锚点是否仍属于同一笔订单业务。
     *
     * @param message 当前支付消息
     * @param trade 已存在的交易锚点
     * @return true-属于同一笔交易
     */
    private boolean isSameTrade(GroupOrderPaidMessage message, GroupTrade trade) {
        if (message == null || trade == null) {
            return false;
        }
        return hasText(message.getOrderId()) && message.getOrderId().equals(trade.getOrderId());
    }

    /**
     * 校验支付成功消息。
     *
     * @param message 支付成功消息
     */
    private void validatePaidMessage(GroupOrderPaidMessage message) {
        if (message == null || message.getTradeType() == null || !hasText(message.getOrderId())
                || message.getUserId() == null || message.getSkuId() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (GroupTradeType.START == message.getTradeType() && !hasText(message.getActivityId())) {
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": activityId不能为空");
        }
        if (GroupTradeType.JOIN == message.getTradeType() && !hasText(message.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": groupId不能为空");
        }
    }

    /**
     * 判断字符串是否有值。
     *
     * @param value 待判断字符串
     * @return true-有值
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
