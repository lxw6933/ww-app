package com.ww.mall.sensitive;

import com.github.houbb.heaven.util.io.StreamUtil;
import com.github.houbb.sensitive.word.api.IWordAllow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-24 23:46
 * @description:
 */
@Slf4j
@Component
public class MallCustomWordAllow implements IWordAllow {

    @Resource
    private MallSensitiveWordProperties mallSensitiveWordProperties;

    @Override
    public List<String> allow() {
        List<String> customWordAllow = null;
        if (StringUtils.isNoneEmpty(mallSensitiveWordProperties.getAllowFileUrl())) {
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(mallSensitiveWordProperties.getAllowFileUrl());
            } catch (FileNotFoundException e) {
                log.error("自定义敏感文件【{}】读取异常{}", mallSensitiveWordProperties.getAllowFileUrl(), e.getMessage());
                return Collections.emptyList();
            }
            customWordAllow = StreamUtil.readAllLines(fileInputStream);
        }
        return customWordAllow != null ?  customWordAllow : Collections.emptyList();
    }

}
