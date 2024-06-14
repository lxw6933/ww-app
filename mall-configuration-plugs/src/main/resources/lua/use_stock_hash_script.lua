-- 商品库存redis hash key
local hashKey = KEYS[1]
-- 变更数量
local number = tonumber(ARGV[1])
-- 扣减锁定库存
redis.call('HINCRBYFLOAT', hashKey, 'lockStock', -number)
-- 新增使用库存
redis.call('HINCRBYFLOAT', hashKey, 'useStock', number)
return 1