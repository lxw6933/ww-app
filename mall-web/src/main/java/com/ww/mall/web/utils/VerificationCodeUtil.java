package com.ww.mall.web.utils;

import lombok.Data;

import java.security.SecureRandom;

/**
 * @author ww
 * @create 2023-11-06- 11:04
 * @description: 验证码生成工具
 */
@Data
public class VerificationCodeUtil {

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    public static String generateVerificationCode() {
        return generateVerificationCode(CODE_LENGTH);
    }

    public static String generateVerificationCode(int codeLength) {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }

    public static void main(String[] args) {
        String verificationCode = generateVerificationCode();
        System.out.println("生成的验证码是：" + verificationCode);
    }

}
