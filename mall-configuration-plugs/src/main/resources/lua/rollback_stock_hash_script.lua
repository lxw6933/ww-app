-- 商品库存redis hash key
local hashKey = KEYS[1]
-- 回滚数量
local number = tonumber(ARGV[1])
-- 商品锁定库存
local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))
-- 库存不足
if lockStock < number then
    return -1
end
-- 回滚锁定库存
redis.call('HINCRBYFLOAT', hashKey, 'lockStock', -number)
return 1