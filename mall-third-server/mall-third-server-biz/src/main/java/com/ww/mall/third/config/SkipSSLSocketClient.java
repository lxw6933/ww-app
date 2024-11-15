package com.ww.mall.third.config;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author ww
 * @create 2023-07-21- 17:15
 * @description:
 */
public class SkipSSLSocketClient {

    /**
     * 获取TrustManager
     * 对于在Android 10及以上的设备上，会出现设备信任问题[用X509ExtendedTrustManager兼容]
     */
    public static X509ExtendedTrustManager X509 = new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
            // TODO document why this method is empty
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
            // TODO document why this method is empty
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
            // TODO document why this method is empty
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
            // TODO document why this method is empty
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            // TODO document why this method is empty
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            // TODO document why this method is empty
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    /**
     * 获取这个SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{X509}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取HostnameVerifier
     */
    public static HostnameVerifier getHostnameVerifier() {
        return (s, sslSession) -> true;
    }

}
