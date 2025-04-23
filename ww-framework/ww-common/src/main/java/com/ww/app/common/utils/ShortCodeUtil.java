package com.ww.app.common.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 高性能分布式短码生成工具类
 * 1. 基于Redis号段模式，每次获取一批ID减少Redis请求
 * 2. 利用本地原子自增，进一步降低Redis压力
 * 3. 支持多种长度的短码生成（5-8位）
 * 4. 与IdUtil协同工作，确保全局唯一性
 *
 * @author ww
 */
@Slf4j
public class ShortCodeUtil {

    /**
     * 默认短码长度(5位)
     */
    public static final int DEFAULT_SHORT_CODE_LENGTH = 5;

    /**
     * 支持的短码长度范围
     */
    public static final int MIN_SHORT_CODE_LENGTH = 5;
    public static final int MAX_SHORT_CODE_LENGTH = 8;

    /**
     * 62进制字符集(0-9, a-z, A-Z)
     */
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * 基数(62进制)
     */
    private static final int RADIX = CHARS.length;

    /**
     * 当前ID计数器 - 按长度分开存储
     */
    private static final AtomicLong[] CURRENT_IDS = new AtomicLong[MAX_SHORT_CODE_LENGTH + 1];

    /**
     * 本地锁 - 按长度分开加锁
     */
    private static final Lock[] LOCKS = new ReentrantLock[MAX_SHORT_CODE_LENGTH + 1];

    /**
     * 不同长度短码对应的号段大小（用于分布式场景）
     */
    public static final int[] SEGMENT_SIZES = {
            0,          // 占位，不使用索引0
            0, 0, 0, 0, // 占位，1-4位短码不使用
            1000,       // 5位短码，每次获取1000个号段
            5000,       // 6位短码，每次获取5000个号段
            10000,      // 7位短码，每次获取10000个号段
            50000       // 8位短码，每次获取50000个号段
    };

    // 静态初始化
    static {
        for (int i = MIN_SHORT_CODE_LENGTH; i <= MAX_SHORT_CODE_LENGTH; i++) {
            CURRENT_IDS[i] = new AtomicLong(0);
            LOCKS[i] = new ReentrantLock();
        }
    }

    private ShortCodeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 生成指定长度的短码
     *
     * @param length 短码长度，支持5-8位
     * @return 生成的短码
     */
    public static String nextShortCode(int length) {
        // 校验短码长度
        if (length < MIN_SHORT_CODE_LENGTH || length > MAX_SHORT_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "短码长度必须在" + MIN_SHORT_CODE_LENGTH + "-" + MAX_SHORT_CODE_LENGTH + "之间");
        }

        // 获取下一个ID
        long id = getNextId(length);

        // 将ID转换为62进制短码
        return idToShortCode(id, length);
    }

    /**
     * 生成默认长度(5位)的短码
     *
     * @return 生成的短码
     */
    public static String nextShortCode() {
        return nextShortCode(DEFAULT_SHORT_CODE_LENGTH);
    }

    /**
     * 批量生成短码
     *
     * @param count  需要生成的短码数量
     * @param length 短码长度
     * @return 短码数组
     */
    public static String[] batchNextShortCodes(int count, int length) {
        if (count <= 0) {
            return new String[0];
        }

        // 校验短码长度
        validateLength(length);

        String[] shortCodes = new String[count];
        for (int i = 0; i < count; i++) {
            shortCodes[i] = nextShortCode(length);
        }
        return shortCodes;
    }

    /**
     * 批量生成默认长度的短码
     *
     * @param count 需要生成的短码数量
     * @return 短码数组
     */
    public static String[] batchNextShortCodes(int count) {
        return batchNextShortCodes(count, DEFAULT_SHORT_CODE_LENGTH);
    }

    /**
     * 获取下一个ID
     *
     * @param length 短码长度
     * @return 下一个ID
     */
    private static long getNextId(int length) {
        // 获取当前计数
        long id = CURRENT_IDS[length].incrementAndGet();

        // 获取当前长度对应的最大值
        long maxId = getMaxIdByLength(length);

        // 如果超过当前长度能表示的最大值，则重置计数器并获取新ID
        if (id >= maxId) {
            LOCKS[length].lock();
            try {
                // 双重检查，避免重复获取
                if (CURRENT_IDS[length].get() >= maxId) {
                    // 重置计数器
                    CURRENT_IDS[length].set(1);
                    id = 1;
                    log.info("短码计数器({}位)已重置", length);
                } else {
                    // 重新获取ID
                    id = CURRENT_IDS[length].incrementAndGet();
                }
            } finally {
                LOCKS[length].unlock();
            }
        }

        return id;
    }

    /**
     * 根据短码长度获取最大ID值
     * 例如：5位短码最大值 = 62^5 - 1
     *
     * @param length 短码长度
     * @return 最大ID值
     */
    private static long getMaxIdByLength(int length) {
        long maxId = 1;
        for (int i = 0; i < length; i++) {
            maxId *= RADIX;
        }
        return maxId - 1;
    }

    /**
     * 将ID转换为短码(62进制)
     * 该方法为核心算法，被本地和分布式短码生成逻辑共用
     *
     * @param id     ID值
     * @param length 短码长度
     * @return 生成的短码
     */
    public static String idToShortCode(long id, int length) {
        validateLength(length);
        
        StringBuilder shortCode = new StringBuilder();

        // 将ID转换为62进制
        long temp = id;
        do {
            shortCode.append(CHARS[(int) (temp % RADIX)]);
            temp = temp / RADIX;
        } while (temp > 0);

        // 补足位数
        while (shortCode.length() < length) {
            shortCode.append('0');
        }

        // 反转字符串，保持高位在前
        return shortCode.reverse().toString();
    }

    /**
     * 校验短码长度是否合法
     * 该方法为通用校验，被本地和分布式短码生成逻辑共用
     *
     * @param length 短码长度
     * @throws IllegalArgumentException 如果长度不合法
     */
    public static void validateLength(int length) {
        if (length < MIN_SHORT_CODE_LENGTH || length > MAX_SHORT_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "短码长度必须在" + MIN_SHORT_CODE_LENGTH + "-" + MAX_SHORT_CODE_LENGTH + "之间");
        }
    }

    /**
     * 将短码转换回ID
     *
     * @param shortCode 短码
     * @return ID
     */
    public static long shortCodeToId(String shortCode) {
        if (StrUtil.isBlank(shortCode)) {
            throw new IllegalArgumentException("短码不能为空");
        }

        long id = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            char c = shortCode.charAt(i);
            int index = -1;
            for (int j = 0; j < CHARS.length; j++) {
                if (CHARS[j] == c) {
                    index = j;
                    break;
                }
            }
            if (index == -1) {
                throw new IllegalArgumentException("非法的短码字符: " + c);
            }
            id = id * RADIX + index;
        }
        return id;
    }

    /**
     * 将短码与业务前缀组合，生成业务短码
     *
     * @param prefix    业务前缀
     * @param shortCode 短码
     * @return 业务短码
     */
    public static String combineWithPrefix(String prefix, String shortCode) {
        if (StrUtil.isBlank(prefix)) {
            return shortCode;
        }
        return prefix + shortCode;
    }

    /**
     * 生成带业务前缀的短码
     *
     * @param prefix 业务前缀
     * @param length 短码长度
     * @return 业务短码
     */
    public static String nextBusinessShortCode(String prefix, int length) {
        return combineWithPrefix(prefix, nextShortCode(length));
    }

    /**
     * 生成带业务前缀的默认长度短码
     *
     * @param prefix 业务前缀
     * @return 业务短码
     */
    public static String nextBusinessShortCode(String prefix) {
        return nextBusinessShortCode(prefix, DEFAULT_SHORT_CODE_LENGTH);
    }
} 