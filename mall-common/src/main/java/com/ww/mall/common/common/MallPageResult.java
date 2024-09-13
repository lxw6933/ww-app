package com.ww.mall.common.common;

import com.ww.mall.common.exception.ApiException;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2023-07-19- 13:48
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MallPageResult<T> extends MallPage {

    /**
     * 当前页数据集
     */
    private List<T> result;

    /**
     * 总页数
     */
    private Integer totalPage;

    /**
     * 总记录数
     */
    private Integer totalCount;

    public MallPageResult(int pageNum, int pageSize, int totalCount, List<T> result) {
        super(pageNum, pageSize);
        this.totalCount = totalCount;
        this.result = result;
        this.totalPage = totalCount % getPageSize() == 0 ? totalCount / getPageSize() : totalCount / getPageSize() + 1;
    }

    public <P> MallPageResult(int pageNum, int pageSize, int totalCount, List<P> result, Function<P, T> convert) {
        super(pageNum, pageSize);
        if (convert == null) {
            throw new ApiException("数据转换器不能为空");
        }
        this.totalCount = totalCount;
        this.result = result.stream().map(convert).collect(Collectors.toList());
        this.totalPage = totalCount % getPageSize() == 0 ? totalCount / getPageSize() : totalCount / getPageSize() + 1;
    }

    public MallPageResult(MallPage page, List<T> result, int totalCount) {
        super(page.getPageNum(), page.getPageSize());
        this.totalCount = totalCount;
        this.result = result;
        this.totalPage = totalCount % getPageSize() == 0 ? totalCount / getPageSize() : totalCount / getPageSize() + 1;
    }

}
