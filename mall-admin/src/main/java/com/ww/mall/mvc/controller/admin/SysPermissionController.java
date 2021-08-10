package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysPermissionService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysPermissionForm;
import com.ww.mall.mvc.view.query.admin.SysPermissionQuery;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * 后台权限表 - controller
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@RestController
@RequestMapping("admin/sys/permission")
public class SysPermissionController extends AbstractController {

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysPermissionQuery query) {
        PageInfo<SysPermissionVO> pageInfo = sysPermissionService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(sysPermissionService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增权限")
    public R save(@RequestBody @Validated SysPermissionForm form) {
        return R.oks(sysPermissionService.save(form));
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "编辑权限")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysPermissionForm form) {
        return R.oks(sysPermissionService.update(form));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除权限")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysPermissionService.deleteById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除权限")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysPermissionService.batchDelete(form.getIds()));
    }

}
