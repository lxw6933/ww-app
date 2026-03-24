--[[
拼团开团脚本。

功能：
1. 基于全局订单索引做开团幂等。
2. 基于活动用户占位计数做活动维度限购。
3. 原子写入团主状态、成员仓库、团内活跃用户索引、订单索引、过期索引。
4. 原子写入过期索引。

KEYS:
1. group:instance:meta:{groupId}
2. group:instance:member-store:{groupId}
3. group:instance:user-index:{groupId}
4. group:order:index
5. group:activity:active:count
6. group:expiry
ARGV:
1. groupId
2. activityId
3. userId
4. orderId
5. requiredSize
6. spuId
7. memberJson
8. nowMillis
9. expireTimeMillis
10. limitPerUser
11. activityUserCountField，例如 ACT_1001:20001
12. retainSeconds

memberJson 样例：
{"groupInstanceId":"67dd3ac8f5a6f80001a10001","userId":20001,"orderId":"ORDER_10001","skuId":30001,"memberStatus":"JOINED","payAmount":99.00}

]]
local existingGroupId = redis.call('HGET', KEYS[4], ARGV[4])
if existingGroupId then
    return {2, existingGroupId}
end

local activeCount = tonumber(redis.call('HGET', KEYS[5], ARGV[11]) or '0')
local limitPerUser = tonumber(ARGV[10] or '0')
if limitPerUser > 0 and activeCount >= limitPerUser then
    return {-3}
end

redis.call('HSET', KEYS[1],
        'activityId', ARGV[2],
        'leaderUserId', ARGV[3],
        'status', 'OPEN',
        'requiredSize', ARGV[5],
        'currentSize', '1',
        'remainingSlots', tostring(tonumber(ARGV[5]) - 1),
        'expireTime', ARGV[9],
        'completeTime', '',
        'failedTime', '',
        'spuId', ARGV[6],
        'failReason', '',
        'createTime', ARGV[8],
        'updateTime', ARGV[8]
)

redis.call('HSET', KEYS[2], ARGV[4], ARGV[7])
redis.call('HSET', KEYS[3], ARGV[3], ARGV[4])
redis.call('HSET', KEYS[4], ARGV[4], ARGV[1])
redis.call('HINCRBY', KEYS[5], ARGV[11], 1)
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[12]))
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[12]))
redis.call('EXPIRE', KEYS[3], tonumber(ARGV[12]))
redis.call('ZADD', KEYS[6], tonumber(ARGV[9]), ARGV[1])

return {1, ARGV[1]}
