-- 限流脚本
-- KEYS[1]: 限流key
-- ARGV[1]: 窗口大小(秒)
-- ARGV[2]: 最大请求数
-- ARGV[3]: 当前时间戳(毫秒)
-- ARGV[4]: 请求唯一标识(可选)

local key = KEYS[1]
local window = tonumber(ARGV[1])
local max = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
-- 改进唯一ID生成，使用更多的随机性和时间精度
local unique_id = ARGV[4] or string.format("%d:%d:%d", now, math.random(1, 1000000), math.random(1, 1000000))
-- 计算时钟漂移容忍度：窗口期的5%或最多100毫秒，取较小值
local drift_tolerance = math.min(window * 50, 100)
local windowStart = now - (window * 1000)
local cleanupNeeded = false

-- 每10次请求才执行一次清理操作，减少清理频率，提高性能
if math.random(1, 10) == 1 then
    cleanupNeeded = true
end

-- 只有需要清理时才执行清理操作
if cleanupNeeded then
    -- 1. 移除窗口外的旧数据（略微提前一点，确保边界数据被正确清理）
    redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart - drift_tolerance)
end

-- 2. 获取当前窗口内的请求数
local count = redis.call('ZCOUNT', key, windowStart, now)  -- 使用精确窗口期

-- 3. 如果未超限则添加当前请求
if count < max then
    -- 使用ZADD命令添加请求记录
    redis.call('ZADD', key, now, unique_id)
    -- 设置过期时间为窗口期加一个小的冗余，避免过早过期
    redis.call('EXPIRE', key, window + math.ceil(window * 0.1))
    return 1
else
    return 0
end