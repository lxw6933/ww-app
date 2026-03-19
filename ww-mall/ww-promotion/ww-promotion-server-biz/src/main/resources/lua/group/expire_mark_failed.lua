--[[
拼团过期失败脚本。

功能：
1. 只处理仍处于 OPEN 且 expireTime 已到的团。
2. 扫描 member-store，将仍占位成员统一标记为 FAILED_REFUND_PENDING。
3. 释放团内活跃用户索引与活动用户占位计数。
4. 更新团主状态为 FAILED，并从 expiry 中移除。
5. 向 stream:event 追加 GROUP_FAILED 事件。

KEYS:
1. group:instance:meta:{groupId}
2. group:instance:member-store:{groupId}
3. group:instance:user-index:{groupId}
4. group:activity:active:count
5. group:stream:event
6. group:expiry

ARGV:
1. groupId
2. reason
3. nowMillis
4. retainSeconds
5. activityUserCountFieldPrefix，例如 ACT_1001:

失败事件样例：
eventType=GROUP_FAILED,groupId=67dd3ac8f5a6f80001a10001,reason=拼团过期未成团
]]
if redis.call('EXISTS', KEYS[1]) == 0 then
    return {0}
end

local status = redis.call('HGET', KEYS[1], 'status')
if status ~= 'OPEN' then
    redis.call('ZREM', KEYS[6], ARGV[1])
    return {-1, status}
end

local expireTime = tonumber(redis.call('HGET', KEYS[1], 'expireTime') or '0')
if expireTime > 0 and tonumber(ARGV[3]) < expireTime then
    return {-2}
end

local memberEntries = redis.call('HGETALL', KEYS[2])
for i = 1, #memberEntries, 2 do
    local orderId = memberEntries[i]
    local member = cjson.decode(memberEntries[i + 1])
    local memberStatus = member.memberStatus
    if memberStatus == 'JOINED' or memberStatus == 'SUCCESS' then
        member.memberStatus = 'FAILED_REFUND_PENDING'
        member.latestTrajectory = 'GROUP_FAILED'
        member.latestTrajectoryTime = tonumber(ARGV[3])
        redis.call('HSET', KEYS[2], orderId, cjson.encode(member))
        redis.call('HDEL', KEYS[3], tostring(member.userId))
        local countField = ARGV[5] .. tostring(member.userId)
        local latestCount = redis.call('HINCRBY', KEYS[4], countField, -1)
        if latestCount <= 0 then
            redis.call('HDEL', KEYS[4], countField)
        end
    end
end

redis.call('HSET', KEYS[1],
        'status', 'FAILED',
        'currentSize', '0',
        'remainingSlots', '0',
        'failedTime', ARGV[3],
        'failReason', ARGV[2],
        'updateTime', ARGV[3]
)
redis.call('ZREM', KEYS[6], ARGV[1])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4]))
redis.call('EXPIRE', KEYS[3], tonumber(ARGV[4]))
redis.call('XADD', KEYS[5], '*',
        'eventType', 'GROUP_FAILED',
        'groupId', ARGV[1],
        'activityId', redis.call('HGET', KEYS[1], 'activityId'),
        'userId', '',
        'orderId', '',
        'reason', ARGV[2],
        'occurredAt', ARGV[3]
)

return {1, ARGV[1]}
