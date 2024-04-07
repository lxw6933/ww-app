package com.ww.mall.redis;

import com.ww.mall.common.constant.RedisKeyConstant;
import lombok.Data;
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
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MallRedisUtil {

    /**
     * 默认批处理命令数量
     */
    private static final Integer DEFAULT_BATCH_NUM = 1000;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * lua扣减库存
     *
     * @param key key
     * @param decrement 扣减数量
     * @return long
     */
    @SuppressWarnings("all")
    public Long decrementStock(String key, long decrement) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(key);

            String script = "local current_stock = tonumber(redis.call('get', KEYS[1]) or 0);\n" +
                    "if current_stock >= tonumber(ARGV[1]) then\n" +
                    "    redis.call('decrby', KEYS[1], tonumber(ARGV[1]));\n" +
                    "    return current_stock - tonumber(ARGV[1]);\n" +
                    "else\n" +
                    "    return -1;\n" +
                    "end";

            Object result = connection.eval(script.getBytes(), ReturnType.INTEGER, 1, keyBytes,
                    String.valueOf(decrement).getBytes());
            return (Long) result;
        });
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

    /**
     * 批量删除key
     *
     * @param keys 需要删除key的集合
     */
    public void batchRemoveKeys(List<String> keys) {
        List<String> batchKeyList = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            batchKeyList.add(keys.get(i));
            if (i % DEFAULT_BATCH_NUM == 0) {
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    batchKeyList.forEach(key -> connection.del(key.getBytes()));
                    // 异步删除，防止bigKey阻塞
//                    batchKeyList.forEach(key -> connection.unlink(key.getBytes()));
                    return null;
                });
                batchKeyList.clear();
            }
        }
        // 处理剩余的命令
        if (!batchKeyList.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                batchKeyList.forEach(key -> connection.del(key.getBytes()));
                // 异步删除，防止bigKey阻塞
//                batchKeyList.forEach(key -> connection.unlink(key.getBytes()));
                return null;
            });
        }
    }

    /**
     * 批量初始化数据
     *
     * @param dataMap data
     */
    public void batchInitializeData(Map<String, String> dataMap) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            dataMap.forEach((key, value) -> connection.set(key.getBytes(), value.getBytes()));
            return null;
        });
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
            typeIdLocations.forEach(res -> {
                locationGEOList.add(new RedisGeoCommands.GeoLocation<>(res.getTypeId().toString(), new Point(res.getX(), res.getY())));
            });
            redisTemplate.opsForGeo().add(geoKey, locationGEOList);
        });
    }

    /**
     * 查询当前位置附近的type地址
     *
     * @param typeId 需要查看地址的类型【按摩店、洗脚店】
     * @param x 当前经度
     * @param y 当前纬度
     * @param nearDistance 附近多少距离
     * @param page 第几页
     * @param size 每页多少条数据
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

}
