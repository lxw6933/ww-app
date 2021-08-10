package com.ww.mall.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @description: 加密解密工具类
 * @author: ww
 * @create: 2021-06-04 10:55
 */
@Slf4j
public class SecretUtils {

    private SecretUtils() {
    }

    /**
     * AES解密
     *
     * @param encryptStr 密文
     * @param decryptKey 秘钥，必须为16个字符组成
     * @return 明文
     * @throws Exception NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException
     */
    public static String aesDecrypt(String encryptStr, String decryptKey) throws Exception {
        if (StringUtils.isEmpty(encryptStr) || StringUtils.isEmpty(decryptKey)) {
            return null;
        }
        byte[] encryptByte = Base64.getDecoder().decode(encryptStr);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey.getBytes(), "AES"));
        byte[] decryptBytes = cipher.doFinal(encryptByte);
        return new String(decryptBytes, StandardCharsets.UTF_8);
    }

    /**
     * AES加密
     *
     * @param content    明文
     * @param encryptKey 秘钥，必须为16个字符组成
     * @return 密文
     * @throws Exception NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException
     */
    public static String aesEncrypt(String content, String encryptKey) throws Exception {
        if (StringUtils.isEmpty(content) || StringUtils.isEmpty(encryptKey)) {
            return null;
        }

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey.getBytes(), "AES"));

        byte[] encryptStr = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptStr);
    }

    public static void main(String[] args) {
        try {
            String secretKey = "ww6933@sina.com.";
            String content = "加密解密";
            String s1 = aesEncrypt(content, secretKey);
            log.info(s1);
            String s = aesDecrypt(s1, secretKey);
            log.info(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
