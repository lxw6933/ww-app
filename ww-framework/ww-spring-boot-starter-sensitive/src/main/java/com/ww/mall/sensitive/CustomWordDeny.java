package com.ww.mall.sensitive;

import com.github.houbb.heaven.util.io.StreamUtil;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.ww.mall.sensitive.config.SensitiveWordProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-24 23:46
 * @description:
 */
@Slf4j
@Component
public class CustomWordDeny implements IWordDeny {

    @Resource
    private SensitiveWordProperties sensitiveWordProperties;

    @Override
    public List<String> deny() {
        List<String> customWordDeny = null;
        if (StringUtils.isNoneEmpty(sensitiveWordProperties.getDenyFileUrl())) {
            try (FileInputStream fileInputStream = new FileInputStream(sensitiveWordProperties.getDenyFileUrl())) {
                customWordDeny = StreamUtil.readAllLines(fileInputStream);
            } catch (Exception e) {
                log.error("自定义敏感文件【{}】读取异常{}", sensitiveWordProperties.getDenyFileUrl(), e.getMessage());
                return Collections.emptyList();
            }
        }
        return customWordDeny != null ?  customWordDeny : Collections.emptyList();
    }

}
