package com.ww.app.admin.controller;

import com.ww.app.operatelog.view.query.SysOperateLogMongoPageQuery;
import com.ww.app.operatelog.view.vo.OperateLogVO;
import com.ww.app.common.common.AppPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "系统后台操作日志记录 API")
public class OperateLogController extends AbstractController {

    @GetMapping("/logs/page")
    @Operation(summary = "操作记录列表")
    public AppPageResult<OperateLogVO> page(SysOperateLogMongoPageQuery query) {
        return sf.getOperateLogService().page(query, userId -> sf.getSysUserService().getById(userId).getRealName());
    }

}
