package com.ww.mall.common.utils.mini;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * @description: 小程序解密前端手机号等信息数据
 * @author: ww
 * @create: 2021-06-21 09:17
 */
public class MiniAesUtils {

    private MiniAesUtils() {}

    /*
     * 解决java.security.NoSuchAlgorithmException: Cannot find any provider supporting AES/CBC/PKCS7Padding
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String wxDecrypt(String encrypted, String sessionKey, String iv) throws Exception {
        byte[] encryptData = Base64.decodeBase64(encrypted);
        byte[] ivData = Base64.decodeBase64(iv);
        byte[] sKey = Base64.decodeBase64(sessionKey);
        return decrypt(sKey, ivData, encryptData);
    }

    public static String decrypt(byte[] key, byte[] iv, byte[] encData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        //解析解密后的字符串
        return new String(cipher.doFinal(encData), StandardCharsets.UTF_8);
    }

}
