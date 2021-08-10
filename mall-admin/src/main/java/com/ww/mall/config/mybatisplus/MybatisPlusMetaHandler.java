package com.ww.mall.config.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2021-04-16 10:23
 */
@Slf4j
@Component
public class MybatisPlusMetaHandler implements MetaObjectHandler {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";
    private static final String CREATE_BY = "createBy";
    private static final String UPDATE_BY = "updateBy";

    @Override
    public void insertFill(MetaObject metaObject) {
        Class<?> createClassType = metaObject.getGetterType(CREATE_TIME);
        if(createClassType == Date.class) {
            this.strictInsertFill(metaObject, CREATE_TIME, Date.class, new Date());
        }else if(createClassType == LocalDateTime.class) {
            this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
        }
        this.strictInsertFill(metaObject, CREATE_BY, Long.class, null);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Class<?> updateClassType = metaObject.getGetterType(UPDATE_TIME);
        if(updateClassType == Date.class) {
            this.strictUpdateFill(metaObject, UPDATE_TIME, Date.class, new Date());
        }else if(updateClassType == LocalDateTime.class) {
            this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        }
        this.strictUpdateFill(metaObject, UPDATE_BY, Long.class, null);
    }

}
