package com.ww.app.admin.utils;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.MD5;

/**
 * @author ww
 * @create 2024-05-20- 17:32
 * @description:
 */
public class PasswordUtil {

    public static final String DEFAULT_PASSWORD = "123456";

    private static final int WORKLOAD = 12;

    private PasswordUtil() {}

    /**
     * 重置密码
     *
     * @param resetSalt resetSalt
     * @return 重置密码
     */
    public static String resetPassword(String resetSalt) {
        String md5Password = MD5.create().digestHex(DEFAULT_PASSWORD);
        return generatePassword(md5Password, resetSalt);
    }

    /**
     * 随机生成用户salt
     *
     * @return salt
     */
    public static String generateSalt() {
        return BCrypt.gensalt(WORKLOAD);
    }

    /**
     * 生成加密的密码
     *
     * @param userMd5Password 用户md5加密密码
     * @return 加密后的密码
     */
    public static String generatePassword(String userMd5Password, String salt) {
        return BCrypt.hashpw(userMd5Password, salt).replace(salt, "");
    }

    /**
     * 验证密码是否匹配
     *
     * @param userMd5Password 用户md加密密码
     * @param hashedPassword 加密后的密码
     * @return 如果匹配返回true，否则返回false
     */
    public static boolean checkPassword(String userMd5Password, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            throw new IllegalArgumentException("无效密码");
        }
        return BCrypt.checkpw(userMd5Password, hashedPassword);
    }

    // 用于测试的main方法
    public static void main(String[] args) {
        String userPassword = "admin";

        String plainPassword = MD5.create().digestHex(userPassword);
        String salt = generateSalt();

        String hashedPassword = PasswordUtil.generatePassword(plainPassword, salt);

        System.out.println("明文密码: " + plainPassword);
        System.out.println("salt：" + salt);
        System.out.println("加密后的密码: " + hashedPassword);

        boolean isMatch = PasswordUtil.checkPassword(MD5.create().digestHex("admin"), salt + hashedPassword);
        System.out.println("密码匹配: " + isMatch);

        System.out.println("====================");
        String resetSalt = generateSalt();
        String res = resetPassword(resetSalt);
        System.out.println("重置结果：" + checkPassword(MD5.create().digestHex(DEFAULT_PASSWORD), resetSalt + res));
    }

}
