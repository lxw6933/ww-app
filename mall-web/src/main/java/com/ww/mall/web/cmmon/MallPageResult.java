package com.ww.mall.web.cmmon;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

    public MallPageResult(IPage<T> page) {
        super((int) page.getCurrent(), (int) page.getSize());
        this.totalCount = (int) page.getTotal();
        this.totalPage = (int) page.getPages();
        this.result = page.getRecords();
    }

    public <P> MallPageResult(IPage<P> page, Function<P, T> convert) {
        super((int) page.getCurrent(), (int) page.getSize());
        if (convert == null) {
            throw new ApiException("数据转换器不能为空");
        }
        this.totalCount = (int) page.getTotal();
        this.totalPage = (int) page.getPages();
        this.result = page.getRecords().stream().map(convert).collect(Collectors.toList());
    }

}
