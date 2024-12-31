package com.ww.app.redis;

import com.ww.app.redis.component.key.GeoRedisKeyBuilder;
import com.ww.app.redis.vo.ActivityHashStockInitBO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static com.ww.app.common.utils.CollectionUtils.convertGroupListMap;

@Slf4j
@Component
public class AppRedisTemplate {

    /**
     * 默认批处理命令数量
     */
    private static final Integer DEFAULT_BATCH_NUM = 1000;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private GeoRedisKeyBuilder geoRedisKeyBuilder;

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

    public boolean initializeBitmap(String key, List<Integer> dataList) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Integer index : dataList) {
                connection.setBit(key.getBytes(), index, true);
            }
            return null;
        });
        return true;
    }

    /**
     * 初始化geo相关数据
     *
     * @param locations DataList
     */
    public void loadGEOData(List<Location> locations) {
        Map<Long, List<Location>> locationMap = convertGroupListMap(locations, Location::getTypeId);
        locationMap.forEach((typeId, typeIdLocations) -> {
            String geoKey = geoRedisKeyBuilder.buildGeoKey(typeId);
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
        String geoKey = geoRedisKeyBuilder.buildGeoKey(typeId);
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
        if (!StringUtils.isNotEmpty(channel)) {
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

    /**
     * 批量初始化hash数据【事务】
     *
     * @param initBOList initBOList
     * @return boolean
     */
    public boolean initializeHashStockInTransaction(List<ActivityHashStockInitBO> initBOList) {
        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) {
                operations.multi();
                initBOList.forEach(initBO -> {
                    initBO.getDataMap().forEach(
                            (field, stock) -> operations.opsForHash().putAll(initBO.getStockHashKey(), initBO.getDataMap())
                    );
                });
                operations.exec();
                return null;
            }
        });
        return true;
    }

}
