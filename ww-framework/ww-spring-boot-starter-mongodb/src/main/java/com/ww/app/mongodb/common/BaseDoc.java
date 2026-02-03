package com.ww.app.mongodb.common;

import com.ww.app.common.constant.Constant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2024-08-23- 18:09
 * @description: MongoDB文档基础类
 */
@Data
public class BaseDoc implements Serializable {

    @Id
    private String id;

    private Date createTime;

    private Date updateTime;

    public static Query buildIdQuery(String id) {
        return new Query().addCriteria(Criteria.where(Constant.MONGO_PRIMARY_KEY).is(id));
    }

    public static Query buildIdListQuery(List<String> idList) {
        return new Query().addCriteria(Criteria.where(Constant.MONGO_PRIMARY_KEY).in(idList));
    }

}
