package com.ww.mall.mongodb.common;

import com.ww.mall.common.constant.Constant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.Serializable;

/**
 * @author ww
 * @create 2024-08-23- 18:09
 * @description:
 */
@Data
public class BaseDoc implements Serializable {

    @Id
    private String id;

    private String createTime;

    private String updateTime;

    public static Query buildIdQuery(String id) {
        return new Query().addCriteria(Criteria.where(Constant.MONGO_PRIMARY_KEY).is(id));
    }

}
