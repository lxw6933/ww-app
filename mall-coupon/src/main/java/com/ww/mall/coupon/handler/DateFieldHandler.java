package com.ww.mall.coupon.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Author:         ww
 * Datetime:       2021\3\8 0008
 * Description:    数据库时间字段自动填充处理器
 */
@Slf4j
@Component
public class DateFieldHandler implements MetaObjectHandler {

    public void insertFill(MetaObject metaObject) {
        log.info("=========insert============");
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    public void updateFill(MetaObject metaObject) {
        log.info("=========update============");
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
