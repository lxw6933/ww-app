package com.ww.mall.web.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author ww
 * @create 2023-11-05- 14:37
 * @description:
 */
@Slf4j
public class AesUtil {

    private static final String KEY = "ww1025";

    public static String encrypt(String encryptContent) {
        return encrypt(KEY, encryptContent);
    }

    public static String decrypt(String decryptContent) {
        return decrypt(KEY, decryptContent);
    }

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

    }

}
