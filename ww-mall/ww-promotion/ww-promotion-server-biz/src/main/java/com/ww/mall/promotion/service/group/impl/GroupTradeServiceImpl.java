package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.constants.GroupTextConstants;
import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.GroupPaymentCallbackRequest;
import com.ww.mall.promotion.controller.app.group.req.JoinGroupRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupTrade;
import com.ww.mall.promotion.enums.GroupFlowSource;
import com.ww.mall.promotion.enums.GroupFlowStage;
import com.ww.mall.promotion.enums.GroupFlowStatus;
import com.ww.mall.promotion.enums.GroupTradeStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.GroupTradeService;
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;

/**
 * 拼团交易编排服务实现。
 * <p>
 * 该服务仅负责承接支付成功回调后的正式业务编排：
 * 1. 以 `group_trade` 作为回调幂等锚点。
 * 2. 以 `group_flow_log` 记录跨边界关键检查点与失败。
 * 3. 复用既有拼团核心服务，不在回调层重复实现并发控制逻辑。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 拼团支付回调编排服务实现
 */
@Slf4j
@Service
public class GroupTradeServiceImpl implements GroupTradeService {

    @Resource
    private GroupInstanceService groupInstanceService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupFlowLogSupport groupFlowLogSupport;

    @Override
    public GroupInstanceVO handlePaymentCallback(GroupPaymentCallbackRequest request) {
        validateCallbackRequest(request);
        String traceId = hasText(request.getTraceId()) ? request.getTraceId() : groupFlowLogSupport.createTraceId();
        String lockKey = groupRedisKeyBuilder.buildPaymentCallbackLockKey(resolveCallbackBizKey(request));
        Boolean locked = Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, traceId, Duration.ofSeconds(GroupBizConstants.PAY_CALLBACK_LOCK_SECONDS)));
        if (!locked) {
            GroupInstanceVO duplicatedResult = tryLoadProcessedGroup(request);
            if (duplicatedResult != null) {
                return duplicatedResult;
            }
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": " + GroupTextConstants.PAY_CALLBACK_IN_PROGRESS);
        }

        try {
            groupFlowLogSupport.record(traceId, request.getGroupId(), request.getActivityId(), request.getUserId(),
                    request.getOrderId(), GroupFlowStage.PAY_CALLBACK, GroupFlowSource.GROUP_PAYMENT_CALLBACK,
                    GroupFlowStatus.PROCESSING, null, null, request);

            GroupTrade existingTrade = findTrade(request);
            if (existingTrade != null && GroupTradeStatus.SUCCESS == existingTrade.getStatus()
                    && hasText(existingTrade.getGroupId())) {
                return groupInstanceService.getGroupDetail(existingTrade.getGroupId());
            }

            saveTrade(traceId, request, null, GroupTradeStatus.PROCESSING, null);
            GroupInstanceVO result = doHandleCallback(request);
            saveTrade(traceId, request, result, GroupTradeStatus.SUCCESS, null);
            groupFlowLogSupport.record(traceId, result.getId(), result.getActivityId(), request.getUserId(),
                    request.getOrderId(), GroupFlowStage.PAY_CALLBACK, GroupFlowSource.GROUP_PAYMENT_CALLBACK,
                    GroupFlowStatus.SUCCESS, null, null, request);
            return result;
        } catch (ApiException e) {
            GroupInstanceVO duplicatedResult = tryLoadProcessedGroup(request);
            if (duplicatedResult != null) {
                saveTrade(traceId, request, duplicatedResult, GroupTradeStatus.SUCCESS,
                        GroupTextConstants.IDEMPOTENT_REPLAY_RETURNED);
                return duplicatedResult;
            }
            saveTrade(traceId, request, null, GroupTradeStatus.FAILED, e.getMessage());
            groupFlowLogSupport.recordFailure(traceId, request.getGroupId(), request.getActivityId(),
                    request.getUserId(), request.getOrderId(), GroupFlowStage.PAY_CALLBACK,
                    GroupFlowSource.GROUP_PAYMENT_CALLBACK, e.getMessage(), request);
            throw e;
        } catch (Exception e) {
            log.error("处理拼团支付回调异常: orderId={}, payTransId={}, tradeType={}",
                    request.getOrderId(), request.getPayTransId(), request.getTradeType(), e);
            saveTrade(traceId, request, null, GroupTradeStatus.FAILED, e.getMessage());
            groupFlowLogSupport.recordFailure(traceId, request.getGroupId(), request.getActivityId(),
                    request.getUserId(), request.getOrderId(), GroupFlowStage.PAY_CALLBACK,
                    GroupFlowSource.GROUP_PAYMENT_CALLBACK, e.getMessage(), request);
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 执行正式业务动作。
     *
     * @param request 支付回调请求
     * @return 拼团详情
     */
    private GroupInstanceVO doHandleCallback(GroupPaymentCallbackRequest request) {
        if (GroupTradeType.START == request.getTradeType()) {
            CreateGroupRequest createRequest = new CreateGroupRequest();
            createRequest.setActivityId(request.getActivityId());
            createRequest.setUserId(request.getUserId());
            createRequest.setOrderId(request.getOrderId());
            createRequest.setPayTransId(request.getPayTransId());
            createRequest.setOrderInfo(hasText(request.getOrderInfo())
                    ? request.getOrderInfo() : GroupBizConstants.EMPTY_ORDER_INFO_JSON);
            return groupInstanceService.createGroup(createRequest);
        }

        JoinGroupRequest joinRequest = new JoinGroupRequest();
        joinRequest.setGroupId(request.getGroupId());
        joinRequest.setUserId(request.getUserId());
        joinRequest.setOrderId(request.getOrderId());
        joinRequest.setPayTransId(request.getPayTransId());
        joinRequest.setOrderInfo(hasText(request.getOrderInfo())
                ? request.getOrderInfo() : GroupBizConstants.EMPTY_ORDER_INFO_JSON);
        return groupInstanceService.joinGroup(joinRequest);
    }

    /**
     * 尝试加载已处理成功的拼团结果。
     *
     * @param request 支付回调请求
     * @return 拼团详情，未命中时返回 null
     */
    private GroupInstanceVO tryLoadProcessedGroup(GroupPaymentCallbackRequest request) {
        GroupTrade trade = findTrade(request);
        if (trade != null && GroupTradeStatus.SUCCESS == trade.getStatus() && hasText(trade.getGroupId())) {
            return groupInstanceService.getGroupDetail(trade.getGroupId());
        }
        String mappedGroupId = stringRedisTemplate.opsForValue()
                .get(groupRedisKeyBuilder.buildOrderMappingKey(request.getOrderId()));
        return hasText(mappedGroupId) ? groupInstanceService.getGroupDetail(mappedGroupId) : null;
    }

    /**
     * 查找历史交易单。
     *
     * @param request 回调请求
     * @return 交易单
     */
    private GroupTrade findTrade(GroupPaymentCallbackRequest request) {
        if (hasText(request.getPayTransId())) {
            GroupTrade byPayTransId = mongoTemplate.findOne(
                    GroupTrade.buildPayTransIdQuery(request.getPayTransId()), GroupTrade.class);
            if (byPayTransId != null) {
                return byPayTransId;
            }
        }
        return hasText(request.getOrderId())
                ? mongoTemplate.findOne(GroupTrade.buildOrderIdQuery(request.getOrderId()), GroupTrade.class)
                : null;
    }

    /**
     * 保存交易单。
     *
     * @param traceId 链路追踪ID
     * @param request 支付回调请求
     * @param result 处理结果
     * @param status 交易状态
     * @param failReason 失败原因
     */
    private void saveTrade(String traceId, GroupPaymentCallbackRequest request, GroupInstanceVO result,
                           GroupTradeStatus status, String failReason) {
        GroupTrade trade = findTrade(request);
        Date now = new Date();
        if (trade == null) {
            trade = new GroupTrade();
            trade.setCreateTime(now);
        }
        trade.setTraceId(traceId);
        trade.setTradeType(request.getTradeType());
        trade.setStatus(status);
        trade.setActivityId(result != null ? result.getActivityId() : request.getActivityId());
        trade.setGroupId(result != null ? result.getId() : request.getGroupId());
        trade.setUserId(request.getUserId());
        trade.setOrderId(request.getOrderId());
        trade.setPayTransId(request.getPayTransId());
        trade.setOrderInfo(request.getOrderInfo());
        trade.setCallbackTime(now);
        trade.setFailReason(failReason);
        trade.setUpdateTime(now);
        mongoTemplate.save(trade);
    }

    /**
     * 校验支付回调入参。
     *
     * @param request 回调请求
     */
    private void validateCallbackRequest(GroupPaymentCallbackRequest request) {
        if (request == null || request.getTradeType() == null || !hasText(request.getOrderId())
                || !hasText(request.getPayTransId()) || request.getUserId() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (GroupTradeType.START == request.getTradeType() && !hasText(request.getActivityId())) {
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": "
                    + GroupTextConstants.START_CALLBACK_ACTIVITY_ID_REQUIRED);
        }
        if (GroupTradeType.JOIN == request.getTradeType() && !hasText(request.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": "
                    + GroupTextConstants.JOIN_CALLBACK_GROUP_ID_REQUIRED);
        }
    }

    /**
     * 构造回调幂等键。
     *
     * @param request 回调请求
     * @return 幂等业务键
     */
    private String resolveCallbackBizKey(GroupPaymentCallbackRequest request) {
        return hasText(request.getPayTransId()) ? request.getPayTransId() : request.getOrderId();
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
