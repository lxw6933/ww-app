package com.ww.mall.sensitive;

import cn.hutool.extra.tokenizer.Word;
import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordData;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.data.WordDatas;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.ignore.SensitiveWordCharIgnores;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import com.ww.mall.sensitive.aspect.SensitiveWordAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.RamUsageEstimator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-24- 17:54
 * @description:
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MallSensitiveWordProperties.class)
public class MallSensitiveWordConfiguration implements DisposableBean {

    @Bean
    public MallCustomWordDeny mallCustomWordDeny() {
        return new MallCustomWordDeny();
    };

    @Bean
    public MallCustomWordAllow mallCustomWordAllow() {
        return new MallCustomWordAllow();
    };

    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        return SensitiveWordBs.newInstance()
                // 不是一个敏感词数据集合
                .wordAllow(WordAllows.chains(WordAllows.defaults(), mallCustomWordAllow()))
                // 是一个敏感词数据集合
                .wordDeny(WordDenys.chains(WordDenys.defaults(), mallCustomWordDeny()))
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
    }

    @Bean
    public SensitiveWordAspect sensitiveWordAspect() {
        return new SensitiveWordAspect();
    }

    @Override
    public void destroy() throws Exception {
        sensitiveWordBs().destroy();
    }
}
