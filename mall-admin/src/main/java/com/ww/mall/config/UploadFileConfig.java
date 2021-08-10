package com.ww.mall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 上传文件配置
 * @author: ww
 * @create: 2021-05-17 16:00
 */
@Configuration
public class UploadFileConfig {

    @Value("${file.uploadType}")
    public String uploadType;

    @Value("${file.savePath.mappingUrl}")
    public String mappingUrl;

    @Value("${file.savePath.windowsUrl}")
    public String windowsUrl;

    @Value("${file.savePath.linuxUrl}")
    public String linuxUrl;

    public String getFileSavePathByEvn() {
        return  System.getProperty("os.name").startsWith("Windows") ? windowsUrl :  linuxUrl;
    }
}
