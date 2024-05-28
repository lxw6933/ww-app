package com.ww.mall.common.utils;

import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * @author ww
 * @create 2024-05-25 11:12
 * @description: 签名工具类
 */
@Slf4j
public class DigitalSignatureUtil {

    /**
     * @param signatureContent  签名内容
     * @param privateKeyEncoded 私钥
     * @return 签名
     */
    public static String generationSignature(String signatureContent, byte[] privateKeyEncoded) {
        try {
            // 创建key的工厂
            KeyFactory keyFactory = KeyFactory.getInstance(Constant.RSA);
            // 创建已编码的私钥规格
            PKCS8EncodedKeySpec encPriKeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
            // 获取指定算法的密钥工厂, 根据已编码的私钥规格, 生成私钥对象
            PrivateKey privateKey = keyFactory.generatePrivate(encPriKeySpec);
            Signature signature = Signature.getInstance(Constant.RSA_SIGNATURE_ALGORITHMS);
            signature.initSign(privateKey);
            signature.update(signatureContent.getBytes());
            byte[] sign = signature.sign();
            // 采用base64算法进行转码,避免出现中文乱码
            return Base64.encodeBase64String(sign);
        } catch (Exception e) {
            log.info("生成签名异常：{}", e.getMessage());
            throw new ApiException("生成签名异常");
        }
    }

    /**
     * 校验签名
     *
     * @param signatureContent 签名内容
     * @param signature        签名
     * @param publicKeyEncoded 公钥
     * @return boolean
     */
    public static boolean verifySignature(String signatureContent, String signature, byte[] publicKeyEncoded) {
        try {
            // 创建key的工厂
            KeyFactory keyFactory = KeyFactory.getInstance(Constant.RSA);
            // 创建已编码的公钥规格
            X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(publicKeyEncoded);
            // 获取指定算法的密钥工厂, 根据已编码的公钥规格, 生成公钥对象
            PublicKey publicKey = keyFactory.generatePublic(encPubKeySpec);
            Signature verifySignature = Signature.getInstance(Constant.RSA_SIGNATURE_ALGORITHMS);
            verifySignature.initVerify(publicKey);
            verifySignature.update(signatureContent.getBytes());
            return verifySignature.verify(Base64.decodeBase64(signature));
        } catch (Exception e) {
            log.info("验签异常：{}", e.getMessage());
            throw new ApiException("验签异常");
        }
    }

    /**
     * 生成签名的规则：签名的内容、RSA签名私钥
     * 效验签名的规则：签名的内容、生成好的签名、RSA签名公钥
     */
    public static void main(String[] args) {
        HashMap<String, String> rsaMap = RSAUtil.generatePublicPrivateKeys();
        String publicKey = rsaMap.get(Constant.RSA_PUBLIC_KEY);
        String privateKey = rsaMap.get(Constant.RSA_PRIVATE_KEY);
        System.out.println("签名公钥：【" + publicKey + "】");
        System.out.println("签名私钥：【" + privateKey + "】");

        String content = "我有一条小毛驴，我从来也不骑";
        // 签名
        String signature = DigitalSignatureUtil.generationSignature(content, Base64.decodeBase64(privateKey));
        // 校验签名
        boolean verifySignature = DigitalSignatureUtil.verifySignature(content, signature, Base64.decodeBase64(publicKey));
        System.out.println("--------------------------生成的签名-----------------");
        System.out.println(signature);
        System.out.println("--------------------------效验签名、正确true、错误false-----------------");
        System.out.println(verifySignature);
    }

}
