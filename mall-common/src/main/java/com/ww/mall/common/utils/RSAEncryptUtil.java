package com.ww.mall.common.utils;

import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * @author ww
 * @create 2024-05-25 11:17
 * @description:
 */
@Slf4j
public class RSAEncryptUtil {

    /**
     * 生成公钥，私钥
     *
     * @return map
     */
    public static HashMap<String, String> generatePublicPrivateKeys() {
        return generatePublicPrivateKeys(2048);
    }

    public static HashMap<String, String> generatePublicPrivateKeys(int length) {
        try {
            HashMap<String, String> keys = new HashMap<>();
            // KeyPairGenerator:秘钥对生成器对象
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Constant.RSA_KEY_ALGORITHMS);
            keyPairGenerator.initialize(length);
            // 生成密钥对
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            // 生成公钥
            PublicKey publicKey = keyPair.getPublic();
            // 生成私钥
            PrivateKey privateKey = keyPair.getPrivate();
            // 获取公销字节数组
            byte[] publicKeyEncoded = publicKey.getEncoded();
            // 获取私销的字节数组
            byte[] privateKeyEncoded = privateKey.getEncoded();
            // 使用base64进行编码
            String privateEncodeStr = Base64.encodeBase64String(privateKeyEncoded);
            String publicEncodeStr = Base64.encodeBase64String(publicKeyEncoded);
            // 打印公钥和私钥
            log.info("生成的公钥: 【{}】", publicEncodeStr);
            log.info("生成的私钥: 【{}】", privateEncodeStr);
            keys.put(Constant.RSA_PUBLIC_KEY, publicEncodeStr);
            keys.put(Constant.RSA_PRIVATE_KEY, privateEncodeStr);
            return keys;
        } catch (Exception e) {
            log.info("生成rsa公钥秘钥异常：{}", e.getMessage());
            throw new ApiException("生成公钥秘钥异常");
        }
    }

    /**
     * @param content          未加密的内容
     * @param publicKeyEncoded 公钥
     * @return rsa加密内容
     */
    public static String encrypt(String content, byte[] publicKeyEncoded) {
        try {
            // 创建key的工厂
            KeyFactory keyFactory = KeyFactory.getInstance(Constant.RSA_KEY_ALGORITHMS);
            // 创建已编码的公钥规格
            X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(publicKeyEncoded);
            // 获取指定算法的密钥工厂, 根据已编码的公钥规格, 生成公钥对象
            PublicKey publicKey = keyFactory.generatePublic(encPubKeySpec);
            // 获取指定算法的密码器
            Cipher cipher = Cipher.getInstance(Constant.RSA_KEY_ALGORITHMS);
            // 初始化密码器（公钥加密模型）
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            // 加密数据, 返回加密后的密文
            byte[] cipherData = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            // 采用base64算法进行转码,避免出现中文乱码
            return Base64.encodeBase64String(cipherData);
        } catch (Exception e) {
            log.info("rsa加密异常：{}", e.getMessage());
            throw new ApiException("rsa加密异常");
        }
    }

    /**
     * @param encodeEncryptContent 加密的内容
     * @param privateKeyEncoded    私钥
     * @return rsa解密内容
     */
    public static String decrypt(String encodeEncryptContent, byte[] privateKeyEncoded) {
        try {
            // 创建key的工厂
            KeyFactory keyFactory = KeyFactory.getInstance(Constant.RSA_KEY_ALGORITHMS);
            // 创建已编码的私钥规格
            PKCS8EncodedKeySpec encPriKeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
            // 获取指定算法的密钥工厂, 根据已编码的私钥规格, 生成私钥对象
            PrivateKey privateKey = keyFactory.generatePrivate(encPriKeySpec);
            // 获取指定算法的密码器
            Cipher cipher = Cipher.getInstance(Constant.RSA_KEY_ALGORITHMS);
            // 初始化密码器（私钥解密模型）
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            // 解密数据, 返回解密后的明文
            byte[] plainData = cipher.doFinal(Base64.decodeBase64(encodeEncryptContent));
            // 采用base64算法进行转码,避免出现中文乱码
            return new String(plainData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.info("rsa解密异常：{}", e.getMessage());
            throw new ApiException("rsa解密异常");
        }
    }

    /**
     * 加密规则：内容、RSA公钥
     * 解密的规则：加密的内容、RSA私钥
     */
    public static void main(String[] args) {
        HashMap<String, String> rsaMap = RSAEncryptUtil.generatePublicPrivateKeys();
        String publicKey = rsaMap.get(Constant.RSA_PUBLIC_KEY);
        String privateKey = rsaMap.get(Constant.RSA_PRIVATE_KEY);
        System.out.println("公钥：【" + publicKey + "】");
        System.out.println("私钥：【" + privateKey + "】");

        String content = "我有一条小毛驴，我从来也不骑";
        // rsa加密
        String encrypt = RSAEncryptUtil.encrypt(content, Base64.decodeBase64(publicKey));
        // rsa解密
        String decrypt = RSAEncryptUtil.decrypt(encrypt, Base64.decodeBase64(privateKey));

        System.out.println("-----------------加密------------------");
        System.out.println(encrypt);
        System.out.println("-----------------解密------------------");
        System.out.println(decrypt);
    }

}
