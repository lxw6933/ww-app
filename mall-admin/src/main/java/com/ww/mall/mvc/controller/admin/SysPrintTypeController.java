package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysPrintTypeService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysPrintTypeForm;
import com.ww.mall.mvc.view.query.admin.SysPrintTypeQuery;
import com.ww.mall.mvc.view.vo.admin.SysPrintTypeVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:49
 */
@RestController
@RequestMapping("/admin/sys/print/type")
public class SysPrintTypeController extends AbstractController {

    @Resource
    private SysPrintTypeService sysPrintTypeService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysPrintTypeQuery query) {
        PageInfo<SysPrintTypeVO> pageInfo = sysPrintTypeService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(sysPrintTypeService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增PDF打印模板类型")
    public R save(@RequestBody @Validated SysPrintTypeForm form) {
        return R.oks(sysPrintTypeService.save(form));
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "编辑PDF打印模板类型")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysPrintTypeForm form) {
        return R.oks(sysPrintTypeService.update(form));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除PDF打印模板类型")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysPrintTypeService.deleteById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除PDF打印模板类型")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysPrintTypeService.batchDelete(form.getIds()));
    }

}

