local key = KEYS[1]
local maxCount = tonumber(ARGV[1])
local second = tonumber(ARGV[2])

local count = redis.call('GET', key)
if count then
    count = tonumber(count)
    if count < maxCount then
        count = count + 1
        redis.call('SET', key, count)
        redis.call('EXPIRE', key, second)
    else
        return false
    end
else
    redis.call('SET', key, 1)
    redis.call('EXPIRE', key, second)
end
return true
