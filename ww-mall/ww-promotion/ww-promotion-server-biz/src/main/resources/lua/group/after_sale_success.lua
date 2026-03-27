--[[
拼团售后成功脚本。

功能：
1. 基于订单定位成员并记录 afterSaleId。
2. 团仍 OPEN 时：
   普通成员售后 -> 释放名额；
   团长售后 -> 整团关闭并标记其他成员待退款。
3. 团已 SUCCESS/FAILED 时只补售后标记，不再改团主状态。
4. 原子维护团内活跃用户索引和过期索引。

KEYS:
1. group:instance:meta:{groupId}
2. group:instance:member-store:{groupId}
3. group:instance:user-index:{groupId}
4. group:expiry
ARGV:
1. groupId
2. afterSaleId
3. orderId
4. nowMillis
5. failReason
6. reason
7. terminalRetainSeconds

]]
if redis.call('EXISTS', KEYS[1]) == 0 then
    return {-1}
end

local targetJson = redis.call('HGET', KEYS[2], ARGV[3])
if not targetJson then
    return {-2}
end

local currentStatus = redis.call('HGET', KEYS[1], 'status')
local leaderUserId = redis.call('HGET', KEYS[1], 'leaderUserId')
local target = cjson.decode(targetJson)
if currentStatus ~= 'OPEN' then
    return {3, currentStatus}
end

if target.afterSaleId and target.afterSaleId ~= '' then
    return {2, currentStatus}
end

target.afterSaleId = ARGV[2]
redis.call('HSET', KEYS[2], ARGV[3], cjson.encode(target))

if leaderUserId and tostring(target.userId) == tostring(leaderUserId) then
    local memberEntries = redis.call('HGETALL', KEYS[2])
    for i = 1, #memberEntries, 2 do
        local orderId = memberEntries[i]
        local member = cjson.decode(memberEntries[i + 1])
        local memberStatus = member.memberStatus
        local active = memberStatus == 'JOINED' or memberStatus == 'SUCCESS'
        if orderId == ARGV[3] then
            member.memberStatus = 'LEADER_AFTER_SALE_CLOSED'
        elseif active then
            member.memberStatus = 'FAILED_REFUND_PENDING'
        end
        if active then
            redis.call('HDEL', KEYS[3], tostring(member.userId))
        end
        redis.call('HSET', KEYS[2], orderId, cjson.encode(member))
    end
    redis.call('HSET', KEYS[1],
            'status', 'FAILED',
            'currentSize', '0',
            'remainingSlots', '0',
            'failedTime', ARGV[4],
            'failReason', ARGV[5],
            'updateTime', ARGV[4]
    )
    redis.call('ZREM', KEYS[4], ARGV[1])
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[7]))
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[7]))
    redis.call('EXPIRE', KEYS[3], tonumber(ARGV[7]))
    return {1, 'FAILED'}
end

local memberStatus = target.memberStatus
if memberStatus == 'JOINED' or memberStatus == 'SUCCESS' then
    target.memberStatus = 'AFTER_SALE_RELEASED'
    redis.call('HSET', KEYS[2], ARGV[3], cjson.encode(target))
    redis.call('HDEL', KEYS[3], tostring(target.userId))
    redis.call('HINCRBY', KEYS[1], 'currentSize', -1)
    redis.call('HINCRBY', KEYS[1], 'remainingSlots', 1)
    redis.call('HSET', KEYS[1], 'updateTime', ARGV[4])
    return {1, currentStatus}
end

return {2, currentStatus}
