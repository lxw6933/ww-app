package com.ww.mall.redis;

import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.redis.constant.LuaConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MallRedisTemplate {

    /**
     * 默认批处理命令数量
     */
    private static final Integer DEFAULT_BATCH_NUM = 1000;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private DefaultRedisScript<Long> decrementStockScript;

    @Resource
    private DefaultRedisScript<Long> lockHashStockScript;

    @Resource
    private DefaultRedisScript<Long> useHashStockScript;

    private String decrementStockSha1;

    private String lockHashStockSha1;

    private String useHashStockSha1;

    private String rollbackHashStockSha1;

    @PostConstruct
    public void init() {
        // 预加载的lua脚本
        decrementStockSha1 = redisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.DECREMENT_STOCK_LUA_BYTE));
        lockHashStockSha1 = redisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.LOCK_STOCK_HASH_LUA_BYTE));
        useHashStockSha1 = redisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.USE_STOCK_HASH_LUA_BYTE));
        rollbackHashStockSha1 = redisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.ROLLBACK_STOCK_HASH_LUA_BYTE));
    }

    /**
     * lua扣减库存
     *
     * @param key    key
     * @param number 扣减数量
     * @return long
     */
    @SuppressWarnings("all")
    public boolean decrementStock(String key, int number) {
        Long res = redisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(key);
            // 执行lua脚本
            Object result = connection.evalSha(decrementStockSha1, ReturnType.INTEGER, 1, keyBytes,
                    String.valueOf(number).getBytes());
            return (Long) result;
        });
        return res != null && res >= 0;
    }

    /**
     * 锁定库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return success > 0
     */
    @SuppressWarnings("all")
    public boolean lockHashStock(String hashKey, int number) {
        Long res = redisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(hashKey);
            // 执行lua脚本
            Object result = connection.evalSha(lockHashStockSha1, ReturnType.INTEGER, 1, keyBytes,
                    String.valueOf(number).getBytes());
            return (Long) result;
        });
        return res != null && res >= 0;
    }

    /**
     * 使用库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return success > 0
     */
    @SuppressWarnings("all")
    public boolean useHashStock(String hashKey, int number) {
        Long res = redisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(hashKey);
            // 执行lua脚本
            Object result = connection.evalSha(useHashStockSha1, ReturnType.INTEGER, 1, keyBytes, String.valueOf(number).getBytes());
            return (Long) result;
        });
        return res != null && res >= 0;
    }

    /**
     * 回滚库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return boolean
     */
    @SuppressWarnings("all")
    public boolean rollbackHashStock(String hashKey, int number) {
        Long res = redisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(hashKey);
            // 执行lua脚本
            Object result = connection.evalSha(rollbackHashStockSha1, ReturnType.INTEGER, 1, keyBytes, String.valueOf(number).getBytes());
            return (Long) result;
        });
        return res != null && res >= 0;
    }

    public boolean a(String key, int number) {
        Long res = redisTemplate.execute(decrementStockScript, Collections.singletonList(key), String.valueOf(number));
        return res != null && res >= 0;
    }

    public boolean b(String key, int number) {
        Long res = redisTemplate.execute(lockHashStockScript, Collections.singletonList(key), String.valueOf(number));
        return res != null && res >= 0;
    }

    public boolean c(String key, int number) {
        Long res = redisTemplate.execute(useHashStockScript, Collections.singletonList(key), String.valueOf(number));
        return res != null && res >= 0;
    }

    /**
     * 批量根据正则表达式扫描匹配的key
     *
     * @param pattern key正则表达式
     * @return List
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute(connect -> {
            Set<String> binaryKeys = new HashSet<>();
            Cursor<byte[]> cursor = connect.scan(
                    ScanOptions.scanOptions().match(pattern).count(100000).build()
            );
            while (cursor.hasNext() && binaryKeys.size() < 100000) {
                binaryKeys.add(new String(cursor.next()));
            }
            keys.addAll(binaryKeys);
            return binaryKeys;
        }, true);
        return keys;
    }

    public void batchRemoveKeys(List<String> keys) {
        this.batchRemoveKeys(keys, false);
    }

    /**
     * 批量删除key
     *
     * @param keys 需要删除key的集合
     */
    public void batchRemoveKeys(List<String> keys, boolean async) {
        List<String> batchKeyList = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            batchKeyList.add(keys.get(i));
            if (i % DEFAULT_BATCH_NUM == 0) {
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    if (async) {
                        // 异步删除，防止bigKey阻塞
                        batchKeyList.forEach(key -> connection.unlink(key.getBytes()));
                    } else {
                        batchKeyList.forEach(key -> connection.del(key.getBytes()));
                    }
                    return null;
                });
                batchKeyList.clear();
            }
        }
        // 处理剩余的命令
        if (!batchKeyList.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                if (async) {
                    // 异步删除，防止bigKey阻塞
                    batchKeyList.forEach(key -> connection.unlink(key.getBytes()));
                } else {
                    batchKeyList.forEach(key -> connection.del(key.getBytes()));
                }
                return null;
            });
        }
    }

    /**
     * 批量初始化数据
     *
     * @param dataMap data
     */
    public void batchInitializeStrData(Map<String, String> dataMap) {
        Map<String, String> batchDataMap = new HashMap<>();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            batchDataMap.put(entry.getKey(), entry.getValue());
            if (batchDataMap.size() == DEFAULT_BATCH_NUM) {
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    batchDataMap.forEach((key, value) -> connection.set(key.getBytes(), value.getBytes()));
                    return null;
                });
                batchDataMap.clear();
            }
        }
        if (!batchDataMap.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                batchDataMap.forEach((key, value) -> connection.set(key.getBytes(), value.getBytes()));
                return null;
            });
        }
    }

    /**
     * 初始化geo相关数据
     *
     * @param locations DataList
     */
    public void loadGEOData(List<Location> locations) {
        Map<Long, List<Location>> locationMap = locations.stream().collect(Collectors.groupingBy(Location::getTypeId));
        locationMap.forEach((typeId, typeIdLocations) -> {
            String geoKey = RedisKeyConstant.GEO_KEY + typeId;
            List<RedisGeoCommands.GeoLocation<String>> locationGEOList = new ArrayList<>();
            typeIdLocations.forEach(res -> locationGEOList.add(new RedisGeoCommands.GeoLocation<>(res.getTypeId().toString(), new Point(res.getX(), res.getY()))));
            redisTemplate.opsForGeo().add(geoKey, locationGEOList);
        });
    }

    /**
     * 查询当前位置附近的type地址
     *
     * @param typeId       需要查看地址的类型【按摩店、洗脚店】
     * @param x            当前经度
     * @param y            当前纬度
     * @param nearDistance 附近多少距离
     * @param page         第几页
     * @param size         每页多少条数据
     * @return map
     */
    public Map<String, Distance> queryNearbyLocation(Long typeId, Double x, Double y, Double nearDistance, Integer page, Integer size) {
        if (x == null || y == null) {
            return Collections.emptyMap();
        }
        int from = (page - 1) * size;
        int end = page * size;
        String geoKey = RedisKeyConstant.GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(geoKey,
                GeoReference.fromCoordinate(x, y),
                new Distance(nearDistance),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        if (results == null) {
            return Collections.emptyMap();
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() < from) {
            return Collections.emptyMap();
        }
        // key： typeId  value：距离多长
        HashMap<String, Distance> distanceMap = new HashMap<>(list.size());
        // 手动分页
        list.stream().skip(from).forEach(res -> {
            String typeIdStr = res.getContent().getName();
            Distance distance = res.getDistance();
            distanceMap.put(typeIdStr, distance);
        });
        return distanceMap;
    }

    @Data
    public static class Location {
        // 地址类型
        public Long typeId;
        // 经度
        public Double x;
        // 纬度
        public Double y;
    }

    /**
     * 发布订阅
     *
     * @param channel 发布渠道
     * @param message 发布消息
     */
    public boolean publishMessage(String channel, String message) {
        if (!StringUtils.hasText(channel)) {
            return false;
        }
        try {
            redisTemplate.convertAndSend(channel, message);
            log.info("发布消息成功,channel：{},message：{}", channel, message);
            return true;
        } catch (Exception e) {
            log.error("发布消息失败,channel：{},message：{}", channel, message);
            return false;
        }
    }

}
