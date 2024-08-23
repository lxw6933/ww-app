package com.ww.mall.mongodb;

import cn.hutool.core.date.DatePattern;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author ww
 * @create 2024-08-23- 18:10
 * @description:
 */
@Component
public class BaseDocListener extends AbstractMongoEventListener<BaseDoc> {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);

    @Override
    public void onBeforeSave(@NotNull BeforeSaveEvent<BaseDoc> event) {
        super.onBeforeSave(event);
        LocalDateTime now = LocalDateTime.now();
        BaseDoc baseDoc = event.getSource();
        if (baseDoc.getCreateTime() == null) {
            baseDoc.setCreateTime(dateFormatter.format(now));
        }
        baseDoc.setUpdateTime(dateFormatter.format(now));
    }

}
