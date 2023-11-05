package com.ww.mall.web.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

import java.nio.charset.StandardCharsets;

/**
 * @author ww
 * @create 2023-11-05- 14:37
 * @description:
 */
public class AesUtil {

    public static String encrypt(String key, String encryptContent) {
        // 生成密钥
        byte[] bytes = getSecretKeyBytes(key);
        // 构建
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, bytes);
        // 加密为16进制表示
        return aes.encryptHex(encryptContent);
    }

    public static String decrypt(String key, String decryptContent) {
        // 生成密钥
        byte[] bytes = getSecretKeyBytes(key);
        // 构建
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, bytes);
        // 解密为字符串
        return aes.decryptStr(decryptContent, CharsetUtil.CHARSET_UTF_8);
    }

    private static byte[] getSecretKeyBytes(String key) {
        // 在密钥生成时必须为128/192/256 bits（位），使用256位
        // byte一字节，8位，故需要达到256位，需要32字节
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 32) {
            byte[] b = new byte[32];
            if (bytes.length < 32) {
                System.arraycopy(bytes, 0, b, 0, bytes.length);
            }
            bytes = b;
        }
        return bytes;
    }

    public static void main(String[] args) {
        System.out.println(encrypt("ww2023", "AA10Ab25Cd"));
        System.out.println(decrypt("ww2023", "eb91e344b6d801fb82136034b121fd07"));
    }

}
