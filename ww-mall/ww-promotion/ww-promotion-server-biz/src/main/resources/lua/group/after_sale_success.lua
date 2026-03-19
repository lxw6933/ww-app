--[[
拼团售后成功脚本。

功能：
1. 基于订单定位成员，记录 afterSaleId 并刷新最近轨迹。
2. 团仍 OPEN 时：
   普通成员售后 -> 释放名额；
   团长售后 -> 整团关闭并标记其他成员待退款。
3. 团已 SUCCESS/FAILED 时只补审计事件，不再改团主状态。
4. 原子维护团内活跃用户索引、活动用户占位计数、过期索引、stream:event。

KEYS:
1. group:instance:meta:{groupId}
2. group:instance:member-store:{groupId}
3. group:instance:user-index:{groupId}
4. group:activity:active:count
5. group:expiry
6. group:stream:event

ARGV:
1. groupId
2. activityId
3. afterSaleId
4. orderId
5. nowMillis
6. failReason
7. reason
8. retainSeconds
9. activityUserCountFieldPrefix，例如 ACT_1001:

事件样例：
GROUP_MEMBER_AFTER_SALE_RELEASED
GROUP_MEMBER_AFTER_SALE_AUDITED
GROUP_FAILED
]]
if redis.call('EXISTS', KEYS[1]) == 0 then
    return {-1}
end

local targetJson = redis.call('HGET', KEYS[2], ARGV[4])
if not targetJson then
    return {-2}
end

local currentStatus = redis.call('HGET', KEYS[1], 'status')
local target = cjson.decode(targetJson)
if target.afterSaleId and target.afterSaleId ~= '' then
    return {2, currentStatus}
end

target.afterSaleId = ARGV[3]
target.latestTrajectory = 'AFTER_SALE_SUCCESS'
target.latestTrajectoryTime = tonumber(ARGV[5])
redis.call('HSET', KEYS[2], ARGV[4], cjson.encode(target))

if currentStatus ~= 'OPEN' then
    redis.call('XADD', KEYS[6], '*',
            'eventType', 'GROUP_MEMBER_AFTER_SALE_AUDITED',
            'groupId', ARGV[1],
            'activityId', ARGV[2],
            'userId', tostring(target.userId or ''),
            'orderId', ARGV[4],
            'reason', ARGV[7],
            'occurredAt', ARGV[5]
    )
    return {1, currentStatus}
end

local function release_activity_count(userId)
    local countField = ARGV[9] .. tostring(userId)
    local latestCount = redis.call('HINCRBY', KEYS[4], countField, -1)
    if latestCount <= 0 then
        redis.call('HDEL', KEYS[4], countField)
    end
end

if tonumber(target.isLeader or 0) == 1 then
    local memberEntries = redis.call('HGETALL', KEYS[2])
    for i = 1, #memberEntries, 2 do
        local orderId = memberEntries[i]
        local member = cjson.decode(memberEntries[i + 1])
        local memberStatus = member.memberStatus
        local active = memberStatus == 'JOINED' or memberStatus == 'SUCCESS'
        if orderId == ARGV[4] then
            member.memberStatus = 'LEADER_AFTER_SALE_CLOSED'
            member.latestTrajectory = 'GROUP_FAILED'
            member.latestTrajectoryTime = tonumber(ARGV[5])
        elseif active then
            member.memberStatus = 'FAILED_REFUND_PENDING'
            member.latestTrajectory = 'GROUP_FAILED'
            member.latestTrajectoryTime = tonumber(ARGV[5])
        end
        if active then
            redis.call('HDEL', KEYS[3], tostring(member.userId))
            release_activity_count(member.userId)
        end
        redis.call('HSET', KEYS[2], orderId, cjson.encode(member))
    end
    redis.call('HSET', KEYS[1],
            'status', 'FAILED',
            'currentSize', '0',
            'remainingSlots', '0',
            'failedTime', ARGV[5],
            'failReason', ARGV[6],
            'updateTime', ARGV[5]
    )
    redis.call('ZREM', KEYS[5], ARGV[1])
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[8]))
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[8]))
    redis.call('EXPIRE', KEYS[3], tonumber(ARGV[8]))
    redis.call('XADD', KEYS[6], '*',
            'eventType', 'GROUP_FAILED',
            'groupId', ARGV[1],
            'activityId', ARGV[2],
            'userId', tostring(target.userId or ''),
            'orderId', ARGV[4],
            'reason', ARGV[6],
            'occurredAt', ARGV[5]
    )
    return {1, 'FAILED'}
end

local memberStatus = target.memberStatus
if memberStatus == 'JOINED' or memberStatus == 'SUCCESS' then
    target.memberStatus = 'AFTER_SALE_RELEASED'
    target.latestTrajectory = 'SEAT_RELEASED'
    target.latestTrajectoryTime = tonumber(ARGV[5])
    redis.call('HSET', KEYS[2], ARGV[4], cjson.encode(target))
    redis.call('HDEL', KEYS[3], tostring(target.userId))
    release_activity_count(target.userId)
    redis.call('HINCRBY', KEYS[1], 'currentSize', -1)
    redis.call('HINCRBY', KEYS[1], 'remainingSlots', 1)
    redis.call('HSET', KEYS[1], 'updateTime', ARGV[5])
    redis.call('XADD', KEYS[6], '*',
            'eventType', 'GROUP_MEMBER_AFTER_SALE_RELEASED',
            'groupId', ARGV[1],
            'activityId', ARGV[2],
            'userId', tostring(target.userId or ''),
            'orderId', ARGV[4],
            'reason', ARGV[7],
            'occurredAt', ARGV[5]
    )
    return {1, currentStatus}
end

redis.call('XADD', KEYS[6], '*',
        'eventType', 'GROUP_MEMBER_AFTER_SALE_AUDITED',
        'groupId', ARGV[1],
        'activityId', ARGV[2],
        'userId', tostring(target.userId or ''),
        'orderId', ARGV[4],
        'reason', ARGV[7],
        'occurredAt', ARGV[5]
)
return {2, currentStatus}
