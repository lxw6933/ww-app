package com.ww.app.common.common;

import com.ww.app.common.exception.ApiException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.function.Function;

import static com.ww.app.common.utils.CollectionUtils.convertList;

/**
 * @author ww
 * @create 2023-07-19- 13:48
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppPageResult<T> extends AppPage {

    @Schema(description = "当前页数据集", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> result;

    @Schema(description = "总页数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalPage;

    @Schema(description = "总记录数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalCount;

    public AppPageResult(int pageNum, int pageSize, int totalCount, List<T> result) {
        super(pageNum, pageSize);
        this.totalCount = totalCount;
        this.result = result;
        this.totalPage = totalCount % getPageSize() == 0 ? totalCount / getPageSize() : totalCount / getPageSize() + 1;
    }

    public <P> AppPageResult(int pageNum, int pageSize, int totalCount, List<P> result, Function<P, T> convert) {
        super(pageNum, pageSize);
        if (convert == null) {
            throw new ApiException("数据转换器不能为空");
        }
        this.totalCount = totalCount;
        this.result = convertList(result, convert);
        this.totalPage = totalCount % getPageSize() == 0 ? totalCount / getPageSize() : totalCount / getPageSize() + 1;
    }

    public AppPageResult(AppPage page, List<T> result, int totalCount) {
        super(page.getPageNum(), page.getPageSize());
        this.totalCount = totalCount;
        this.result = result;
        this.totalPage = totalCount % getPageSize() == 0 ? totalCount / getPageSize() : totalCount / getPageSize() + 1;
    }

}
