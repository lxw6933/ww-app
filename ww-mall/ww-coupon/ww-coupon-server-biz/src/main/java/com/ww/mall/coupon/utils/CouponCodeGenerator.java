package com.ww.mall.coupon.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author ww
 * @create 2025-10-12 18:04
 * @description: 优惠券券码生成器
 * 特性：
 * - 使用 128-bit 随机熵（两个 long） -> 与 UUIDv4 强度一致
 * - Base62 编码，长度约 22（≤ 26）
 * - 无序（不包含时间/顺序信息）
 * - 并发友好、适合大批量生成
 */
public class CouponCodeGenerator {

    // Base62 字符表：0-9 A-Z a-z
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final BigInteger RADIX = BigInteger.valueOf(62L);

    private CouponCodeGenerator() {
        // no instance
    }

    /**
     * 生成单个券码
     *
     * @return 券码字符串
     */
    public static String generate() {
        long hi = ThreadLocalRandom.current().nextLong();
        long lo = ThreadLocalRandom.current().nextLong();
        return encodeBase62FromTwoLongs(hi, lo);
    }

    /**
     * 批量生成券码
     *
     * @param count 要生成的数量，必须 >= 0
     * @return 包含 count 个券码的 List
     */
    public static List<String> generateBatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        List<String> list = new ArrayList<>(Math.max(16, count));
        for (int i = 0; i < count; i++) {
            list.add(generate());
        }
        return list;
    }

    /**
     * 把两个 long 组合成 128-bit（无符号）并 Base62 编码
     */
    private static String encodeBase62FromTwoLongs(long hi, long lo) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(hi);
        buf.putLong(lo);
        byte[] bytes = buf.array();
        BigInteger value = new BigInteger(1, bytes); // 1 表示正数（无符号）
        return toBase62(value);
    }

    /**
     * BigInteger -> Base62 字符串
     */
    private static String toBase62(BigInteger value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (value.signum() == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder(24); // 预分配
        while (value.signum() > 0) {
            BigInteger[] dr = value.divideAndRemainder(RADIX);
            value = dr[0];
            int digit = dr[1].intValue();
            sb.append(BASE62[digit]);
        }
        return sb.reverse().toString();
    }

    // 简单演示 main（实际环境可删除）
    public static void main(String[] args) {
        HashSet<String> target = new HashSet<>(100000000);
        System.out.println("single: " + generate());
        for (int i = 0; i < 100; i++) {
            List<String> codes = generateBatch(100000);
            target.addAll(codes);
            System.out.println("数量：" + target.size());
        }
    }

}
