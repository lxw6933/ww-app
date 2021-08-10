package com.ww.mall.member.config.mybatisplus.page;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @description: 分页对象
 * @author: ww
 * @create: 2021-05-12 19:03
 */
@Data
public class Pagination {
    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 当前显示条数
     */
    private Integer limit;

    /**
     * 排序字段
     */
    private String orderBy;

    public Pagination() {
        this.page = 1;
        this.limit = 10;
    }

    public Pagination(Integer page, Integer limit) {
        if (Objects.nonNull(page) && Objects.nonNull(limit)) {
            this.page = page;
            this.limit = limit;
        }
    }

    public Pagination(Integer page, Integer limit, String orderBy) {
        if (Objects.nonNull(page) && Objects.nonNull(limit)) {
            this.page = page;
            this.limit = limit;
        }
        if (StringUtils.isNotEmpty(orderBy)) {
            this.orderBy = orderBy;
        }
    }

    public Pagination(String orderBy) {
        if (StringUtils.isNotEmpty(orderBy)) {
            this.orderBy = orderBy;
        }
    }

    public void setPage(Integer page) {
        if (Objects.nonNull(page)) {
            this.page = page;
        }
    }

    public void setLimit(Integer limit) {
        if (Objects.nonNull(limit)) {
            this.limit = limit;
        }
    }

    public void setOrderBy(String orderBy) {
        if (StringUtils.isNotEmpty(orderBy)) {
            this.orderBy = orderBy;
        }
    }
}
