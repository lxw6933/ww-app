-- 统一签到脚本（支持正常签到和补签）
local signKey = KEYS[1]
local countKey = KEYS[2]
local offset = tonumber(ARGV[1])
local isResign = tonumber(ARGV[2])
local maxResign = tonumber(ARGV[3])
local expireSeconds = tonumber(ARGV[4])

-- 检查是否已签到
if redis.call('GETBIT', signKey, offset) == 1 then
    return -1
end

-- 补签逻辑
if isResign == 1 then
    local usedCount = tonumber(redis.call('GET', countKey) or '0')
    if usedCount >= maxResign then
        return -2
    end

    if usedCount == 0 then
        redis.call('SET', countKey, '1', 'EX', expireSeconds)
    else
        redis.call('INCR', countKey)
    end
end

-- 执行签到
redis.call('SETBIT', signKey, offset, 1)

return 1
