package com.ww.app.redis;

import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.component.key.GeoRedisKeyBuilder;
import com.ww.app.redis.listener.KeyScanListener;
import com.ww.app.redis.vo.RedisHashInitBO;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.ww.app.common.utils.CollectionUtils.convertGroupListMap;

/**
 * Redis模板工具类
 * 提供批量操作和GEO等高级功能
 *
 * @author ww
 */
@Slf4j
@Component
public class AppRedisTemplate {

    /**
     * 默认批处理命令数量
     */
    private static final int DEFAULT_BATCH_NUM = 1000;
    
    /**
     * 默认扫描数量限制
     */
    private static final int DEFAULT_SCAN_COUNT = 500;
    
    /**
     * 默认过期时间(秒)
     */
    private static final long DEFAULT_EXPIRE_SECONDS = 3600;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GeoRedisKeyBuilder geoRedisKeyBuilder;

    public boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 批量根据正则表达式扫描匹配的key
     *
     * @param pattern key正则表达式
     * @return 匹配的key集合
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> targetKeyList = new HashSet<>();
        this.scanKeys(pattern, new KeyScanListener() {
            @Override
            public void onKey(String key) {
                targetKeyList.add(key);
            }

            @Override
            public void onFinish() {
                log.info("pattern:[{}] key 扫描完毕", pattern);
            }
        });
        return targetKeyList;
    }

    /**
     * 批量根据正则表达式扫描匹配的key
     *
     * @param pattern key正则表达式
     * @param listener key监听器
     */
    public void scanKeys(String pattern, KeyScanListener listener) {
        if (StringUtils.isBlank(pattern)) {
            throw new ApiException("扫描模式不能为空");
        }

        try {
            stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(DEFAULT_SCAN_COUNT).build())) {
                    while (cursor.hasNext()) {
                        listener.onKey(new String(cursor.next()));
                    }
                    listener.onFinish();
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Redis扫描key异常: pattern={}", pattern, e);
            throw new ApiException("Redis扫描key异常: " + e.getMessage());
        }
    }

    /**
     * 批量删除key
     *
     * @param keys 需要删除key的集合
     */
    public void batchRemoveKeys(List<String> keys) {
        batchRemoveKeys(keys, false);
    }

    /**
     * 批量删除key
     *
     * @param keys 需要删除key的集合
     * @param async 是否异步删除
     */
    public void batchRemoveKeys(List<String> keys, boolean async) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        
        try {
            // 分批处理
            List<List<String>> batches = splitList(keys, DEFAULT_BATCH_NUM);
            for (List<String> batch : batches) {
                stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String key : batch) {
                        if (async) {
                            // 异步删除，防止bigKey阻塞
                            connection.unlink(key.getBytes());
                        } else {
                            connection.del(key.getBytes());
                        }
                    }
                    return null;
                });
                log.debug("批量删除key成功: size={}, async={}", batch.size(), async);
            }
        } catch (Exception e) {
            log.error("批量删除key异常: size={}, async={}", keys.size(), async, e);
            throw new ApiException("批量删除key异常: " + e.getMessage());
        }
    }

    /**
     * 批量初始化字符串数据
     *
     * @param dataMap 数据Map
     */
    public void batchInitializeStrData(Map<String, String> dataMap) {
        batchInitializeStrData(dataMap, 0);
    }
    
    /**
     * 批量初始化字符串数据
     *
     * @param dataMap 数据Map
     * @param expireSeconds 过期时间(秒)，0表示不过期
     */
    public void batchInitializeStrData(Map<String, String> dataMap, long expireSeconds) {
        if (dataMap == null || dataMap.isEmpty()) {
            return;
        }
        
        try {
            // 分批处理
            int batchCount = 0;
            Map<String, String> batchDataMap = new HashMap<>(DEFAULT_BATCH_NUM);
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                batchDataMap.put(entry.getKey(), entry.getValue());
                batchCount++;
                
                if (batchCount % DEFAULT_BATCH_NUM == 0) {
                    processBatchStringData(batchDataMap, expireSeconds);
                    batchDataMap.clear();
                }
            }
            
            // 处理剩余的数据
            if (!batchDataMap.isEmpty()) {
                processBatchStringData(batchDataMap, expireSeconds);
            }
            
            log.debug("批量初始化字符串数据成功: size={}", dataMap.size());
        } catch (Exception e) {
            log.error("批量初始化字符串数据异常: size={}", dataMap.size(), e);
            throw new ApiException("批量初始化字符串数据异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理批量字符串数据
     *
     * @param batchDataMap 批量数据Map
     * @param expireSeconds 过期时间(秒)
     */
    private void processBatchStringData(Map<String, String> batchDataMap, long expireSeconds) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Map.Entry<String, String> entry : batchDataMap.entrySet()) {
                byte[] keyBytes = entry.getKey().getBytes();
                byte[] valueBytes = entry.getValue().getBytes();
                connection.set(keyBytes, valueBytes);
                
                if (expireSeconds > 0) {
                    connection.expire(keyBytes, expireSeconds);
                }
            }
            return null;
        });
    }

    /**
     * 初始化Bitmap
     *
     * @param key Bitmap的key
     * @param dataList 位图索引列表
     * @return 是否成功
     */
    public boolean initializeBitmap(String key, List<Integer> dataList) {
        if (StringUtils.isBlank(key) || CollectionUtils.isEmpty(dataList)) {
            return false;
        }
        
        try {
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Integer index : dataList) {
                    if (index != null && index >= 0) {
                        connection.setBit(key.getBytes(), index, true);
                    }
                }
                return null;
            });
            log.debug("初始化Bitmap成功: key={}, size={}", key, dataList.size());
            return true;
        } catch (Exception e) {
            log.error("初始化Bitmap异常: key={}", key, e);
            return false;
        }
    }

    /**
     * 初始化GEO相关数据
     *
     * @param locations 位置数据列表
     */
    public void loadGEOData(List<Location> locations) {
        if (CollectionUtils.isEmpty(locations)) {
            return;
        }
        
        try {
            Map<Long, List<Location>> locationMap = convertGroupListMap(locations, Location::getTypeId);
            locationMap.forEach((typeId, typeIdLocations) -> {
                if (typeId != null && !CollectionUtils.isEmpty(typeIdLocations)) {
                    String geoKey = geoRedisKeyBuilder.buildGeoKey(typeId);
                    List<RedisGeoCommands.GeoLocation<String>> locationGEOList = new ArrayList<>(typeIdLocations.size());
                    
                    for (Location location : typeIdLocations) {
                        if (location.getX() != null && location.getY() != null) {
                            locationGEOList.add(new RedisGeoCommands.GeoLocation<>(
                                    location.getTypeId().toString(), 
                                    new Point(location.getX(), location.getY())
                            ));
                        }
                    }
                    
                    if (!locationGEOList.isEmpty()) {
                        stringRedisTemplate.opsForGeo().add(geoKey, locationGEOList);
                        log.debug("初始化GEO数据成功: typeId={}, size={}", typeId, locationGEOList.size());
                    }
                }
            });
        } catch (Exception e) {
            log.error("初始化GEO数据异常: size={}", locations.size(), e);
            throw new ApiException("初始化GEO数据异常: " + e.getMessage());
        }
    }

    /**
     * 查询指定类型的附近位置
     *
     * @param typeId       位置类型ID
     * @param longitude    当前经度
     * @param latitude     当前纬度
     * @param distance     查询距离
     * @param page         页码
     * @param size         每页大小
     * @return 位置距离Map
     */
    public Map<String, Distance> queryNearbyLocation(Long typeId, Double longitude, Double latitude, 
            Double distance, Integer page, Integer size) {
        // 参数校验
        if (typeId == null || longitude == null || latitude == null || 
                distance == null || page == null || size == null || 
                page < 1 || size < 1) {
            return Collections.emptyMap();
        }
        
        try {
            int from = (page - 1) * size;
            int limit = page * size;
            String geoKey = geoRedisKeyBuilder.buildGeoKey(typeId);
            
            // 执行GEO搜索
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                    geoKey,
                    GeoReference.fromCoordinate(longitude, latitude),
                    new Distance(distance),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(limit)
            );
            
            if (results == null) {
                return Collections.emptyMap();
            }
            
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> resultList = results.getContent();
            if (CollectionUtils.isEmpty(resultList) || resultList.size() < from) {
                return Collections.emptyMap();
            }
            
            // 手动分页并构建结果Map
            Map<String, Distance> distanceMap = new HashMap<>(Math.min(resultList.size() - from, size));
            resultList.stream()
                    .skip(from)
                    .limit(size)
                    .forEach(result -> {
                        String locationId = result.getContent().getName();
                        Distance locationDistance = result.getDistance();
                        distanceMap.put(locationId, locationDistance);
                    });
            
            log.debug("查询附近位置成功: typeId={}, results={}", typeId, distanceMap.size());
            return distanceMap;
        } catch (Exception e) {
            log.error("查询附近位置异常: typeId={}, longitude={}, latitude={}", typeId, longitude, latitude, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 发布消息到频道
     *
     * @param channel 频道名称
     * @param message 消息内容
     * @return 是否成功
     */
    public boolean publishMessage(String channel, String message) {
        if (StringUtils.isBlank(channel) || message == null) {
            log.warn("发布消息参数无效: channel={}", channel);
            return false;
        }
        
        try {
            stringRedisTemplate.convertAndSend(channel, message);
            log.debug("发布消息成功: channel={}, message={}", channel, message);
            return true;
        } catch (Exception e) {
            log.error("发布消息失败: channel={}, message={}", channel, message, e);
            return false;
        }
    }

    /**
     * 在事务中批量初始化Hash数据
     *
     * @param initBOList 初始化数据列表
     * @return 是否成功
     */
    public boolean initializeHashStockInTransaction(List<RedisHashInitBO> initBOList) {
        if (CollectionUtils.isEmpty(initBOList)) {
            return false;
        }
        
        try {
            stringRedisTemplate.execute(new SessionCallback<Void>() {
                @Override
                public Void execute(@NonNull RedisOperations operations) {
                    operations.multi();
                    for (RedisHashInitBO initBO : initBOList) {
                        if (initBO != null && StringUtils.isNotBlank(initBO.getStockHashKey()) && 
                                initBO.getDataMap() != null && !initBO.getDataMap().isEmpty()) {
                            operations.opsForHash().putAll(initBO.getStockHashKey(), initBO.getDataMap());
                        }
                    }
                    operations.exec();
                    return null;
                }
            });
            log.debug("事务中初始化Hash数据成功: size={}", initBOList.size());
            return true;
        } catch (Exception e) {
            log.error("事务中初始化Hash数据异常: size={}", initBOList.size(), e);
            return false;
        }
    }
    
    /**
     * 设置字符串键值并设置过期时间
     *
     * @param key 键
     * @param value 值
     * @param expireSeconds 过期时间(秒)
     * @return 是否成功
     */
    public boolean setWithExpire(String key, String value, long expireSeconds) {
        if (StringUtils.isBlank(key) || value == null) {
            return false;
        }
        
        try {
            if (expireSeconds > 0) {
                stringRedisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置过期键值异常: key={}, expireSeconds={}", key, expireSeconds, e);
            return false;
        }
    }
    
    /**
     * 批量获取值
     *
     * @param keys 键集合
     * @return 值列表
     */
    public List<String> multiGet(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }
        
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            return values != null ? values : Collections.emptyList();
        } catch (Exception e) {
            log.error("批量获取值异常: keys={}", keys, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 批量执行Redis命令
     *
     * @param batchSize 批处理大小
     * @param dataList 数据列表
     * @param function 批处理函数
     * @param <T> 数据类型
     */
    public <T> void batchExecute(int batchSize, List<T> dataList, Function<List<T>, Void> function) {
        if (CollectionUtils.isEmpty(dataList) || function == null) {
            return;
        }
        
        int actualBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_NUM;
        List<List<T>> batches = splitList(dataList, actualBatchSize);
        
        for (List<T> batch : batches) {
            try {
                function.apply(batch);
                log.debug("批量执行Redis命令成功: batchSize={}", batch.size());
            } catch (Exception e) {
                log.error("批量执行Redis命令异常: batchSize={}", batch.size(), e);
                throw new ApiException("批量执行Redis命令异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 将列表按指定大小分割
     *
     * @param list 原始列表
     * @param batchSize 批处理大小
     * @param <T> 列表元素类型
     * @return 分割后的批次列表
     */
    private <T> List<List<T>> splitList(List<T> list, int batchSize) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        
        List<List<T>> result = new ArrayList<>((list.size() + batchSize - 1) / batchSize);
        int size = list.size();
        
        for (int i = 0; i < size; i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, size)));
        }
        
        return result;
    }

    /**
     * 位置数据类
     */
    @Data
    public static class Location {
        /**
         * 位置类型ID
         */
        private Long typeId;
        
        /**
         * 经度
         */
        private Double x;
        
        /**
         * 纬度
         */
        private Double y;
    }
}
