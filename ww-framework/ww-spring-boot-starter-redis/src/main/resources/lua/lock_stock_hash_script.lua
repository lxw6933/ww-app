-- 商品库存redis hash key
local hashKey = KEYS[1]
-- 要扣减的库存数量
local decrementStock = tonumber(ARGV[1])
-- 商品总库存
local totalStock = tonumber(redis.call('HGET', hashKey, 'totalStock'))
-- 商品锁定库存
local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))
-- 商品使用库存
local useStock = tonumber(redis.call('HGET', hashKey, 'useStock'))
-- 如何任何一个字段不存在则扣减失败
if totalStock == nil or lockStock == nil or useStock == nil then
   return -1
end
-- 商品可用库存
local availableStock = totalStock - lockStock - useStock
-- 库存不足
if availableStock < decrementStock then
   return -2
end
-- 更新锁定库存
local newLockStock = lockStock + decrementStock
redis.call('HSET', hashKey, 'lockStock', newLockStock)
return 1
