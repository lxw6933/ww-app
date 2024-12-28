package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.query.SysOperateLogMongoPage;
import com.ww.mall.admin.view.vo.OperateLogVO;
import com.ww.mall.common.common.AppPageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ww
 * @create 2024-09-19- 11:46
 * @description:
 */
@RestController
@RequestMapping("/operateLog")
public class OperateLogController extends AbstractController {

    @GetMapping("/logs/page")
    public AppPageResult<OperateLogVO> page(SysOperateLogMongoPage query) {
        return sf.getOperateLogService().page(query);
    }

}
