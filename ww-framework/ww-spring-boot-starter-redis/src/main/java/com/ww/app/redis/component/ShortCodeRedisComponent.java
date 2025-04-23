package com.ww.app.redis.component;

import cn.hutool.core.util.StrUtil;
import com.ww.app.common.utils.ShortCodeUtil;
import com.ww.app.redis.component.key.ShortCodeRedisKeyBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分布式短码生成组件
 * 基于Redis号段模式和本地原子自增实现
 * 支持多节点并发无碰撞生成短码
 *
 * @author ww
 */
@Slf4j
@Component
public class ShortCodeRedisComponent {

    /**
     * ID获取锁超时时间(毫秒)
     */
    private static final long LOCK_TIMEOUT_MS = 3000L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShortCodeRedisKeyBuilder shortCodeRedisKeyBuilder;

    /**
     * 状态Key生成器
     */
    private static class StateKey {
        private final String businessType;
        private final int length;

        public StateKey(String businessType, int length) {
            this.businessType = businessType;
            this.length = length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateKey stateKey = (StateKey) o;
            return length == stateKey.length && businessType.equals(stateKey.businessType);
        }

        @Override
        public int hashCode() {
            int result = businessType.hashCode();
            result = 31 * result + length;
            return result;
        }
    }

    /**
     * 短码生成器状态 - 按短码长度和业务类型
     */
    @Getter
    public static class ShortCodeState {
        /**
         * 当前服务实例的最大ID
         */
        private final AtomicLong currentMaxId = new AtomicLong(0);

        /**
         * 当前ID计数器
         */
        private final AtomicLong currentId = new AtomicLong(0);

        /**
         * 用于本地加锁
         */
        private final Lock lock = new ReentrantLock();

        /**
         * 是否已初始化
         * -- GETTER --
         * 是否已初始化
         */
        private volatile boolean initialized = false;

        /**
         * 获取当前ID
         */
        public long getCurrentId() {
            return currentId.get();
        }

        /**
         * 获取最大ID
         */
        public long getCurrentMaxId() {
            return currentMaxId.get();
        }

        /**
         * 获取并增加当前ID
         */
        public long incrementAndGetCurrentId() {
            return currentId.incrementAndGet();
        }

        /**
         * 设置当前ID
         */
        public void setCurrentId(long id) {
            currentId.set(id);
        }

        /**
         * 设置最大ID
         */
        public void setCurrentMaxId(long id) {
            currentMaxId.set(id);
        }

        /**
         * 获取锁
         */
        public void lock() {
            lock.lock();
        }

        /**
         * 释放锁
         */
        public void unlock() {
            lock.unlock();
        }

        /**
         * 标记为已初始化
         */
        public void markInitialized() {
            initialized = true;
        }

    }

    /**
     * 维护各业务类型的短码生成器状态
     */
    private final Map<StateKey, ShortCodeState> stateMap = new ConcurrentHashMap<>();

    /**
     * 初始化，记录日志
     */
    @PostConstruct
    public void init() {
        log.info("分布式短码生成器组件已初始化");
    }

    /**
     * 生成下一个短码
     *
     * @param businessType 业务类型，用于隔离不同业务的短码序列
     * @param length       短码长度
     * @return 生成的短码
     */
    public String nextShortCode(String businessType, int length) {
        if (StrUtil.isBlank(businessType)) {
            throw new IllegalArgumentException("业务类型不能为空");
        }

        // 校验短码长度
        ShortCodeUtil.validateLength(length);

        // 获取或创建业务类型对应的状态
        StateKey stateKey = new StateKey(businessType, length);
        ShortCodeState state = getOrCreateState(stateKey);

        // 初始化状态
        initializeStateIfNeeded(state, businessType, length);

        // 获取下一个ID
        long id = getNextId(state, businessType, length);

        // 将ID转换为短码 - 复用ShortCodeUtil的核心算法
        return ShortCodeUtil.idToShortCode(id, length);
    }

    /**
     * 生成默认长度(5位)的短码
     *
     * @param businessType 业务类型
     * @return 生成的短码
     */
    public String nextShortCode(String businessType) {
        return nextShortCode(businessType, ShortCodeUtil.DEFAULT_SHORT_CODE_LENGTH);
    }

    /**
     * 批量生成短码
     *
     * @param businessType 业务类型
     * @param count        数量
     * @param length       长度
     * @return 短码数组
     */
    public String[] batchNextShortCodes(String businessType, int count, int length) {
        if (count <= 0) {
            return new String[0];
        }

        String[] shortCodes = new String[count];
        for (int i = 0; i < count; i++) {
            shortCodes[i] = nextShortCode(businessType, length);
        }
        return shortCodes;
    }

    /**
     * 批量生成默认长度的短码
     *
     * @param businessType 业务类型
     * @param count        数量
     * @return 短码数组
     */
    public String[] batchNextShortCodes(String businessType, int count) {
        return batchNextShortCodes(businessType, count, ShortCodeUtil.DEFAULT_SHORT_CODE_LENGTH);
    }

    /**
     * 重置指定业务类型的短码生成器
     *
     * @param businessType 业务类型
     * @param length       短码长度
     */
    public void reset(String businessType, int length) {
        if (StrUtil.isBlank(businessType)) {
            throw new IllegalArgumentException("业务类型不能为空");
        }

        // 校验短码长度
        ShortCodeUtil.validateLength(length);

        // 获取状态
        StateKey stateKey = new StateKey(businessType, length);
        ShortCodeState state = stateMap.get(stateKey);
        if (state == null) {
            return;
        }

        state.lock();
        try {
            // 构建锁键
            String lockKey = shortCodeRedisKeyBuilder.buildLockKey(businessType, length);

            // 尝试获取分布式锁
            boolean locked = Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey, String.valueOf(System.currentTimeMillis()),
                    Duration.ofMillis(LOCK_TIMEOUT_MS)));

            if (locked) {
                try {
                    // 删除Redis中的最大ID
                    String maxIdKey = shortCodeRedisKeyBuilder.buildMaxIdKey(businessType, length);
                    stringRedisTemplate.delete(maxIdKey);

                    // 重置本地状态
                    state.setCurrentMaxId(0);
                    state.setCurrentId(0);
                    state.markInitialized();

                    log.info("短码生成器已重置: 业务类型={}, 长度={}", businessType, length);
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            } else {
                log.warn("获取分布式锁失败，无法重置短码生成器: 业务类型={}, 长度={}", businessType, length);
                throw new RuntimeException("获取分布式锁失败，无法重置短码生成器");
            }
        } finally {
            state.unlock();
        }
    }

    /**
     * 获取或创建业务类型对应的状态
     */
    private ShortCodeState getOrCreateState(StateKey stateKey) {
        return stateMap.computeIfAbsent(stateKey, k -> new ShortCodeState());
    }

    /**
     * 如果需要，初始化状态
     */
    private void initializeStateIfNeeded(ShortCodeState state, String businessType, int length) {
        if (!state.isInitialized()) {
            state.lock();
            try {
                if (!state.isInitialized()) {
                    // 触发从Redis获取新的号段
                    getNextSegmentFromRedis(state, businessType, length);
                    state.markInitialized();
                }
            } finally {
                state.unlock();
            }
        }
    }

    /**
     * 获取下一个ID
     */
    private long getNextId(ShortCodeState state, String businessType, int length) {
        // 获取当前计数
        long id = state.incrementAndGetCurrentId();

        // 如果超过当前最大ID，则需要从Redis获取新的号段
        if (id > state.getCurrentMaxId()) {
            state.lock();
            try {
                // 双重检查，避免重复获取
                if (id > state.getCurrentMaxId()) {
                    // 从Redis获取新的号段
                    getNextSegmentFromRedis(state, businessType, length);

                    // 重新获取ID
                    id = state.incrementAndGetCurrentId();
                }
            } finally {
                state.unlock();
            }
        }
        return id;
    }

    /**
     * 从Redis获取新的号段
     */
    private void getNextSegmentFromRedis(ShortCodeState state, String businessType, int length) {
        // 获取号段大小 - 从ShortCodeUtil获取配置
        int segmentSize = ShortCodeUtil.SEGMENT_SIZES[length];

        // 构建锁键和最大ID键
        String lockKey = shortCodeRedisKeyBuilder.buildLockKey(businessType, length);
        String maxIdKey = shortCodeRedisKeyBuilder.buildMaxIdKey(businessType, length);

        // 尝试获取分布式锁
        boolean locked = false;
        try {
            // 使用SET NX实现分布式锁
            locked = Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey, String.valueOf(System.currentTimeMillis()),
                    Duration.ofMillis(LOCK_TIMEOUT_MS)));

            if (locked) {
                // 获取当前Redis中的最大ID
                String currentMaxIdStr = stringRedisTemplate.opsForValue().get(maxIdKey);
                long newMaxId;

                if (currentMaxIdStr == null) {
                    // 如果Redis中没有存储最大ID，则初始化
                    newMaxId = segmentSize;
                    stringRedisTemplate.opsForValue().set(maxIdKey, String.valueOf(newMaxId));
                } else {
                    // 使用INCRBY原子增加号段大小
                    newMaxId = stringRedisTemplate.opsForValue().increment(maxIdKey, segmentSize);
                }

                log.info("获取新号段成功: 业务类型={}, 长度={}, 号段=[{}, {}]",
                        businessType, length, newMaxId - segmentSize + 1, newMaxId);

                // 更新本地最大ID和当前ID
                state.setCurrentMaxId(newMaxId);
                state.setCurrentId(newMaxId - segmentSize);
            } else {
                // 如果获取锁失败，等待一段时间后重试
                log.warn("获取分布式锁失败，等待重试: 业务类型={}, 长度={}", businessType, length);
                Thread.sleep(100);
                getNextSegmentFromRedis(state, businessType, length);
            }
        } catch (Exception e) {
            log.error("从Redis获取新号段异常: 业务类型={}, 长度={}", businessType, length, e);
            throw new RuntimeException("获取ID异常", e);
        } finally {
            // 释放分布式锁
            if (locked) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }
} 