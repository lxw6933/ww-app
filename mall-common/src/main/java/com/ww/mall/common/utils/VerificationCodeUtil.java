package com.ww.mall.common.utils;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;

/**
 * @author ww
 * @create 2023-11-06- 11:04
 * @description: 验证码生成工具
 */
@Data
public class VerificationCodeUtil {

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;

    public static String generateVerificationCode() {
        return generateVerificationCode(CODE_LENGTH);
    }

    /**
     * 生产验证码
     *
     * @param codeLength code位数
     * @return code
     */
    public static String generateVerificationCode(int codeLength) {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }

    public static boolean validateCaptcha(String userInputCode, String targetCode) {
        return validCode(userInputCode, targetCode, false);
    }

    /**
     * 校验验证码
     *
     * @param userInputCode 用户输入code
     * @param targetCode 目标code
     * @param flag 是否不区分大小写
     * @return boolean
     */
    public static boolean validCode(String userInputCode, String targetCode, boolean flag) {
        if (StringUtils.isEmpty(userInputCode) || StringUtils.isEmpty(targetCode)) {
            return false;
        }
        if (flag) {
            String userInputCodeLower = userInputCode.toLowerCase();
            String targetCodeLower = targetCode.toLowerCase();
            return userInputCodeLower.equals(targetCodeLower);
        } else {
            return userInputCode.equals(targetCode);
        }
    }

    public static void main(String[] args) {
        String verificationCode = generateVerificationCode();
        System.out.println("生成的验证码是：" + verificationCode);
    }

}
