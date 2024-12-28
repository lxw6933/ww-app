package com.ww.mall.sensitive.config;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.ignore.SensitiveWordCharIgnores;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import com.ww.mall.sensitive.CustomWordAllow;
import com.ww.mall.sensitive.CustomWordDeny;
import com.ww.mall.sensitive.aspect.SensitiveWordAspect;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-05-24- 17:54
 * @description:
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SensitiveWordProperties.class)
public class SensitiveWordAutoConfiguration {

    @Bean
    public CustomWordDeny mallCustomWordDeny() {
        return new CustomWordDeny();
    }

    @Bean
    public CustomWordAllow mallCustomWordAllow() {
        return new CustomWordAllow();
    }

    @Bean
    public SensitiveWordBs sensitiveWordBs(CustomWordDeny customWordDeny, CustomWordAllow customWordAllow) {
        SensitiveWordBs sensitiveWordBs = SensitiveWordBs.newInstance()
                // 不是一个敏感词数据集合
                .wordAllow(WordAllows.chains(WordAllows.defaults(), customWordAllow))
                // 是一个敏感词数据集合
                .wordDeny(WordDenys.chains(WordDenys.defaults(), customWordDeny))
                // 忽略大小写
                .ignoreCase(true)
                // 忽略半角圆角
                .ignoreWidth(true)
                // 忽略数字的写法
                .ignoreNumStyle(true)
                // 忽略中文的书写格式
                .ignoreChineseStyle(true)
                // 忽略英文的书写格式
                .ignoreEnglishStyle(true)
                // 忽略重复词
                .ignoreRepeat(false)
                // 是否启用数字检测
                .enableNumCheck(true)
                // 是有启用邮箱检测
                .enableEmailCheck(true)
                // 是否启用链接检测
                .enableUrlCheck(true)
                // 是否启用敏感单词检测
                .enableWordCheck(true)
                // 数字检测，自定义指定长度
                .numCheckLen(8)
                // 词对应的标签
                .wordTag(WordTags.none())
                // 忽略的字符
                .charIgnore(SensitiveWordCharIgnores.specialChars())
                // 针对匹配的敏感词额外加工，比如可以限制英文单词必须全匹配
                .wordResultCondition(WordResultConditions.englishWordMatch())
                .init();
        long customAllowSize = ObjectSizeCalculator.getObjectSize(customWordAllow);
        log.info("customAllowSize size：【{}】byte", customAllowSize);
        long customDenySize = ObjectSizeCalculator.getObjectSize(customWordDeny);
        log.info("customDenySize size：【{}】byte", customDenySize);
        long sensitiveWordTotalSize = ObjectSizeCalculator.getObjectSize(sensitiveWordBs);
        log.info("sensitiveWordTotalSize size：【{}】byte", sensitiveWordTotalSize);
        return sensitiveWordBs;
    }

    @Bean
    public SensitiveWordAspect sensitiveWordAspect() {
        return new SensitiveWordAspect();
    }

}
