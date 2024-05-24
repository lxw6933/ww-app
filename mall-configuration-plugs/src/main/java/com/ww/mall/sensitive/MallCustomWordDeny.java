package com.ww.mall.sensitive;

import com.github.houbb.heaven.util.io.StreamUtil;
import com.github.houbb.sensitive.word.api.IWordDeny;
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
public class MallCustomWordDeny implements IWordDeny {

    @Resource
    private MallSensitiveWordProperties mallSensitiveWordProperties;

    @Override
    public List<String> deny() {
        List<String> customWordDeny = null;
        if (StringUtils.isNoneEmpty(mallSensitiveWordProperties.getDenyFileUrl())) {
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(mallSensitiveWordProperties.getDenyFileUrl());
            } catch (FileNotFoundException e) {
                log.error("自定义敏感文件【{}】读取异常{}", mallSensitiveWordProperties.getDenyFileUrl(), e.getMessage());
                return Collections.emptyList();
            }
            customWordDeny = StreamUtil.readAllLines(fileInputStream);
        }
        return customWordDeny != null ?  customWordDeny : Collections.emptyList();
    }

}
