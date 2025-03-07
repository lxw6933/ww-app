package com.ww.app.mongodb.listener;

import com.ww.app.mongodb.common.BaseDoc;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author ww
 * @create 2024-08-23- 18:10
 * @description:
 */
@Component
public class BaseDocListener extends AbstractMongoEventListener<BaseDoc> {

    @Override
    public void onBeforeConvert(@NotNull BeforeConvertEvent<BaseDoc> event) {
        BaseDoc baseDoc = event.getSource();
        if (baseDoc.getCreateTime() == null) {
            baseDoc.setCreateTime(new Date());
        }
    }

    @Override
    public void onBeforeSave(@NotNull BeforeSaveEvent<BaseDoc> event) {
        Document document = event.getDocument();
        if (document != null) {
            document.put("updateTime", new Date());
        }
    }

}
