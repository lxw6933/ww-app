--[[
拼团开团脚本。

功能：
1. 基于 groupId 做开团幂等。
2. 原子写入团主状态、成员仓库、团内活跃用户索引、过期索引。

KEYS:
1. group:instance:meta:{groupId}
2. group:instance:member-store:{groupId}
3. group:instance:user-index:{groupId}
4. group:activity:stats:{activityId}
5. group:expiry
ARGV:
1. groupId
2. activityId
3. userId
4. orderId
5. requiredSize
6. spuId
7. memberJson
8. nowMillis
9. businessExpireTimeMillis
10. openCacheTtlSeconds

memberJson 样例：
{"groupInstanceId":"67dd3ac8f5a6f80001a10001","userId":20001,"orderId":"ORDER_10001","skuId":30001,"memberStatus":"JOINED","payAmount":99.00}

]]
if redis.call('EXISTS', KEYS[1]) == 1 then
    local existingOrderId = redis.call('HGET', KEYS[2], ARGV[4])
    if existingOrderId then
        return {2, ARGV[1]}
    end
    return {-1}
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
redis.call('HINCRBY', KEYS[4], 'openGroupCount', 1)
redis.call('HINCRBY', KEYS[4], 'joinMemberCount', 1)
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[10]))
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[10]))
redis.call('EXPIRE', KEYS[3], tonumber(ARGV[10]))
redis.call('ZADD', KEYS[5], tonumber(ARGV[9]), ARGV[1])

return {1, ARGV[1]}
