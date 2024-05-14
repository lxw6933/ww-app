-- 商品库存的 Redis key
local key = KEYS[1]
-- 要扣减的库存数量
local count = tonumber(ARGV[1])

-- 检查库存是否充足
local currentStock = tonumber(redis.call("GET", key) or 0)
if currentStock >= count then
    -- 库存充足，扣减库存
    redis.call("DECRBY", key, count)
    return currentStock - count
else
    -- 库存不足，扣减失败
    return -1
end
