package com.ww.mall.mongodb.listener;

import cn.hutool.core.date.DatePattern;
import com.ww.mall.mongodb.common.BaseDoc;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
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
    public void onBeforeConvert(@NotNull BeforeConvertEvent<BaseDoc> event) {
        LocalDateTime now = LocalDateTime.now();
        BaseDoc baseDoc = event.getSource();
        if (baseDoc.getCreateTime() == null) {
            baseDoc.setCreateTime(dateFormatter.format(now));
        }
    }

    @Override
    public void onBeforeSave(@NotNull BeforeSaveEvent<BaseDoc> event) {
        LocalDateTime now = LocalDateTime.now();
        Document document = event.getDocument();
        if (document != null) {
            document.put("updateTime", dateFormatter.format(now));
        }
    }

}
