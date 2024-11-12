package com.ww.mall.admin.view.query;

import com.ww.mall.admin.entity.mongo.OperateLog;
import com.ww.mall.common.utils.SpecialCharacterUtil;
import com.ww.mall.mongodb.common.AbstractMongoPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * @author ww
 * @create 2024-09-19- 09:25
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperateLogMongoPage extends AbstractMongoPage<OperateLog> {

    /**
     * 操作用户id
     */
    private Long userId;

    /**
     * 业务模块id
     */
    private String bizId;

    /**
     * 业务模块类型
     */
    private String type;

    /**
     * 业务模块子类型
     */
    private String subType;

    /**
     * 操作明细
     */
    private String action;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    @Override
    public Criteria buildQuery() {
        Criteria criteria = new Criteria();
        if (this.userId != null) {
            criteria.and("userId").is(this.userId);
        }
        if (StringUtils.isNotEmpty(this.bizId)) {
            criteria.and("bizId").is(this.bizId);
        }
        if (StringUtils.isNotEmpty(this.type)) {
            String escapedKeyword = SpecialCharacterUtil.escapeSpecialCharacters(this.type);
            String pattern = ".*" + escapedKeyword + ".*";
            criteria.and("type").regex(pattern, "i");
        }
        if (StringUtils.isNotEmpty(this.subType)) {
            String escapedKeyword = SpecialCharacterUtil.escapeSpecialCharacters(this.subType);
            String pattern = ".*" + escapedKeyword + ".*";
            criteria.and("subType").regex(pattern, "i");
        }
        if (StringUtils.isNotEmpty(this.action)) {
            String escapedKeyword = SpecialCharacterUtil.escapeSpecialCharacters(this.action);
            String pattern = ".*" + escapedKeyword + ".*";
            criteria.and("action").regex(pattern, "i");
        }
        if (StringUtils.isNotEmpty(this.startTime) && StringUtils.isNotEmpty(this.endTime)) {
            criteria.andOperator(
                    Criteria.where("createTime").gte(this.startTime),
                    Criteria.where("createTime").lte(this.endTime)
            );
        }
        return criteria;
    }

    @Override
    public Sort buildSort() {
        return Sort.by("id");
    }

}
