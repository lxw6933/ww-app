package com.ww.app.pay.utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * @author ww
 * @create 2026-03-11 11:11
 * @description:
 */
public class WechatPayKeyUtil {

    private static final String BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String END = "-----END PRIVATE KEY-----";

    /**
     * PEM 转单行字符串（存 JSON / DB）
     */
    public static String pemToSingleLine(String pem) {
        return pem.replace("\n", "").replace("\r", "");
    }

    /**
     * 单行字符串转 PEM 格式
     */
    public static String singleLineToPem(String key) {

        key = key.replace(BEGIN, "")
                .replace(END, "")
                .replaceAll("\\s+", "");

        StringBuilder sb = new StringBuilder();
        sb.append(BEGIN).append("\n");

        for (int i = 0; i < key.length(); i += 64) {
            int end = Math.min(i + 64, key.length());
            sb.append(key, i, end).append("\n");
        }

        sb.append(END);
        return sb.toString();
    }

    /**
     * 字符串转 PrivateKey（微信签名用）
     */
    public static PrivateKey loadPrivateKey(String key) throws Exception {

        key = key.replace(BEGIN, "")
                .replace(END, "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);

        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);
    }

    public static void main(String[] args) throws Exception {
        String pem = readFile("E:\\company\\WXCertUtil\\cert\\apiclient_key.pem");
        String source = pemToSingleLine(pem);
        System.out.println(source);
        String target = "-----BEGIN PRIVATE KEY-----MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDlDpks5brFF430584bC8VmN8gdDckDhc7jxKC2ExeQxTq50i/ANX1cD9EgPnVvJZmjU/Lo8lRnIU1JTEO7RK18y3hqeMGYyF+OxxDjXja7dQ1qe5yfLqdAN3V3HkkEsQBrx6pvWcDMxMSfBKgurIQJRYGrgKzGJDfy9VlpK8Dz1KzHfvP4/+WnDFzNIzLPnW6/xvPAYLl3lTyCE+yzOgHYATiHEfHE38Ge8SBBIFTI1vrPJaDholDcXLThtUOhUFGOc/L98dsr0XEiUkVNFXHhJa/eZGcI2LwRwCJbXRBTkVKpmCCJY3jnaEGARg9yZ1Wic/LE74GozaxHRU9vZrdBAgMBAAECggEBAJYCQX6N1QtOqcFCLlC4HMccsQmWBZcxApVYKIgVaNF+2T7o0Czo2vh3w1WCkhYAYeX9DJBSY8RGov7l21XBXHzmJC5WdzM/xpUS+ZRuFxodRnP+EEyPmSkNKPih9OAFq2jqkkvqU8HG+53IxvwKL06DHrDazEvXXGzZEX03El6cJDwWm668D0uJU/VLXXKeb57MefV4A2bA5jSMpwGhn2yOHDjM/ctmJsUP1amdMgvX8qt7PX7OYjTSq3GOG7MvfdRaOgFw5SkgnGJx4oxFhadSOg7UcrI3ACVunsTJsQEjm9t/KFQymliZXuIrZpkdxeRTSxSgG3hbK6xd5mzQYyECgYEA/B3w71dZ6POtgVENWh0lKSirCKsj8+E5+VSIRbtEQFdlRUM8pEOUAZj1TOVOSqJlb87nNZrAC3V5cV9iIja/bicKWa9Kn3mk5cY17zMMjp+AAy6fEZcyJGJgO2RJkOC3Oq5QTqDLp8fO6urQPxPrm/RM42P9MgTGBBq8GgaYTjsCgYEA6JW8QcW5RsJlx3/oHhbXsRzJaWCeND/qenL0zG+5oGXa1hGgxmqzzqc6CCwia6EQQe0fNN4fY9+tY9X2eynkDWtfZbQhHCSo3HW+r7mA4jm6Fs1dNPT0+hkfRM2z+3KJwbcTTwymPGtNnWwHTCq7exBAQg3mhkYJQVjnMZ+jzLMCgYEAt7H9JfaIbSJ3bfyndNw9gkK+c792n7CgNBmyfNRYg2TqRdAatDkE8zEGsjN1mw2+SPwBHN6XRQIgLUnpT7KCQnkxom0FOzM/wadtDs8sPBLdC/SBNCjtAPOo2D1XGLeS6a+ulcu095evHR1gBEW4atZS22+0QSiXOtBb876QZJcCgYEAlObP6HfSXvezu14q16CeJBie5aTig+brkcso5/0bJRIwYN5WRNEpHkM3RuYify9VPi+1y93s0L7xvbnMnBs8kk7Me0sv61mY9dM36j5jwfFdLp35bx7n+3m76Audj3xLr0YqyW/6uTGlNgRkQ6IT9vx3dxJ5JDuQT1LAaKegcTsCgYEAhT7kXwy9yE3xMVbUiOvoYARBTKLI7DpAaT1dhADsZrG7MCDrmLwaZlvIg9Pjdwl5GYpMLZ4aKXyzBe0QTZ3mszSqIhG+/V85FJMHvIOg5h4XOHCYbe7eGNNER0wbQykj4vFx46J2vFDlvHZ1WJNRJFxLEDlB/yW74KLY96BrUzU=-----END PRIVATE KEY-----";
        System.out.println(target);
        System.out.println(target.equals(source));
    }

    public static String readFile(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败: " + path, e);
        }
    }

}
