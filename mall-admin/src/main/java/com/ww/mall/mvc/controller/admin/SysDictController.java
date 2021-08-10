package com.ww.mall.mvc.controller.admin;

import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.valid.group.AddGroup;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.mvc.service.SysDictService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysDictForm;
import com.ww.mall.mvc.view.vo.admin.SysDictVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 字典 controller
 * @author: ww
 * @create: 2021-05-18 17:32
 */
@RestController
@RequestMapping("/admin/sys/dict")
public class SysDictController extends AbstractController {

    @Resource
    private SysDictService sysDictService;

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        return R.oks(sysDictService.getById(id));
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @SysLog(value = "新增字典数据")
    public R save(@RequestBody @Validated({AddGroup.class}) SysDictForm form){
        sysDictService.save(form);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @SysLog(value = "修改字典数据")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysDictForm form){
        sysDictService.update(form);
        return R.ok();
    }

    /**
     * 修改启用状态
     */
    @PostMapping("/updateStatus")
    @SysLog(value = "更改字典数据状态")
    public R updateStatus(@RequestBody @Validated IdForm form){
        sysDictService.updateStatus(form.getId());
        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除字典数据")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysDictService.removeById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除字典数据")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysDictService.batchDelete(form.getIds()));
    }

    /**
     * 获取指定类型字典
     */
    @RequestMapping("/list")
    public R list(String dictType){
        List<SysDictVO> vos = sysDictService.getDict(dictType);
        return R.oks(vos);
    }

}
