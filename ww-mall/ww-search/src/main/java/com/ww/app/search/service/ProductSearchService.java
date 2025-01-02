package com.ww.app.search.service;

import com.ww.app.common.common.AppPageResult;
import com.ww.app.search.entity.es.ProductDoc;
import com.ww.app.search.view.dto.ProductSearchPageDTO;

/**
 * @author ww
 * @create 2025-01-02- 10:58
 * @description:
 */
public interface ProductSearchService {

    AppPageResult<ProductDoc> search(ProductSearchPageDTO dto);

}
