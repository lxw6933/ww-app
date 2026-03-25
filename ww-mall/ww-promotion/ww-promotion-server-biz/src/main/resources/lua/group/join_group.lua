--[[
拼团参团脚本。

功能：
1. 基于全局订单索引做参团幂等与串单校验。
2. 基于团主状态判断 OPEN/业务失效/剩余名额。
3. 基于团内活跃用户索引判断“是否已在该团占位”。
4. 原子写入成员、索引、团主状态。
5. 满团时批量把所有 JOINED 成员升级为 SUCCESS。

KEYS:
1. group:instance:meta:{groupId}
2. group:instance:member-store:{groupId}
3. group:instance:user-index:{groupId}
4. group:order:index
5. group:activity:stats:{activityId}
6. group:expiry
ARGV:
1. groupId
2. activityId
3. userId
4. orderId
5. memberJson
6. nowMillis
7. terminalRetainSeconds

返回值样例：
{1, groupId, GROUP_JOINED, currentSize, remainingSlots}
{1, groupId, GROUP_COMPLETED, currentSize, remainingSlots}
{2, groupId} 表示幂等回放
]]
local existingGroupId = redis.call('HGET', KEYS[4], ARGV[4])
if existingGroupId then
    if existingGroupId == ARGV[1] then
        return {2, existingGroupId}
    end
    return {-2, existingGroupId}
end

if redis.call('EXISTS', KEYS[1]) == 0 then
    return {-1}
end

local status = redis.call('HGET', KEYS[1], 'status')
if status ~= 'OPEN' then
    return {-4, status}
end

local expireTime = tonumber(redis.call('HGET', KEYS[1], 'expireTime') or '0')
if expireTime > 0 and tonumber(ARGV[6]) >= expireTime then
    return {-5}
end

if redis.call('HEXISTS', KEYS[3], ARGV[3]) == 1 then
    return {-6}
end

local remainingSlots = tonumber(redis.call('HGET', KEYS[1], 'remainingSlots') or '0')
if remainingSlots <= 0 then
    return {-8}
end

local currentSize = redis.call('HINCRBY', KEYS[1], 'currentSize', 1)
remainingSlots = redis.call('HINCRBY', KEYS[1], 'remainingSlots', -1)
redis.call('HSET', KEYS[1], 'updateTime', ARGV[6])
redis.call('HSET', KEYS[2], ARGV[4], ARGV[5])
redis.call('HSET', KEYS[3], ARGV[3], ARGV[4])
redis.call('HSET', KEYS[4], ARGV[4], ARGV[1])
redis.call('HINCRBY', KEYS[5], 'joinMemberCount', 1)

local eventType = 'GROUP_JOINED'
if tonumber(remainingSlots) == 0 then
    redis.call('HSET', KEYS[1],
            'status', 'SUCCESS',
            'completeTime', ARGV[6],
            'failReason', '',
            'updateTime', ARGV[6]
    )
    local memberEntries = redis.call('HGETALL', KEYS[2])
    for i = 1, #memberEntries, 2 do
        local orderId = memberEntries[i]
        local member = cjson.decode(memberEntries[i + 1])
        if member.memberStatus == 'JOINED' then
            member.memberStatus = 'SUCCESS'
            member.latestTrajectory = 'GROUP_SUCCESS'
            member.latestTrajectoryTime = tonumber(ARGV[6])
            redis.call('HSET', KEYS[2], orderId, cjson.encode(member))
        end
    end
    redis.call('ZREM', KEYS[6], ARGV[1])
    eventType = 'GROUP_COMPLETED'
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[7]))
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[7]))
    redis.call('EXPIRE', KEYS[3], tonumber(ARGV[7]))
end

return {1, ARGV[1], eventType, tostring(currentSize), tostring(remainingSlots)}
