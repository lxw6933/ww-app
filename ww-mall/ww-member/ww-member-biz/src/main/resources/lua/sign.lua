local signKey = KEYS[1]
local countKey = KEYS[2]
local offset = tonumber(ARGV[1])
local maxResign = tonumber(ARGV[2])
local expireSeconds = tonumber(ARGV[3])

-- 检查是否已签到
if redis.call('GETBIT', signKey, offset) == 1 then
    return {-1, '该日期已签到，请勿重新签到'}
end

-- 检查补签次数
local usedCount = tonumber(redis.call('GET', countKey) or '0')
if usedCount >= maxResign then
    return {-2, '本月补签次数已用完'}
end

-- 执行签到
redis.call('SETBIT', signKey, offset, 1)

-- 增加补签次数
local newCount = redis.call('INCR', countKey)

-- 首次设置过期时间
if newCount == 1 then
    redis.call('EXPIRE', countKey, expireSeconds)
end

return {1, '补签成功'}
