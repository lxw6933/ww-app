package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.valid.group.AddGroup;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysDictTypeService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysDictTypeForm;
import com.ww.mall.mvc.view.query.admin.SysDictTypeQuery;
import com.ww.mall.mvc.view.vo.admin.SysDictTypeVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 字典类型 controller
 * @author: ww
 * @create: 2021-05-18 17:53
 */
@RestController
@RequestMapping("/admin/sys/dict/type")
public class SysDictTypeController extends AbstractController {

    @Resource
    private SysDictTypeService sysDictTypeService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysDictTypeQuery query) {
        PageInfo<SysDictTypeVO> pageInfo = sysDictTypeService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(sysDictTypeService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增字典类型")
    public R save(@RequestBody @Validated({AddGroup.class}) SysDictTypeForm form) {
        sysDictTypeService.save(form);
        return R.ok();
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "修改字典类型")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysDictTypeForm form) {
        sysDictTypeService.update(form);
        return R.ok();
    }

    /**
     * 修改启用状态
     */
    @PostMapping("/updateStatus")
    @SysLog(value = "更改字典类型状态")
    public R updateStatus(@RequestBody @Validated IdForm form){
        sysDictTypeService.updateStatus(form.getId());
        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除字典类型")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysDictTypeService.removeById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除字典类型")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysDictTypeService.batchDelete(form.getIds()));
    }

}

