package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysDictEntity;
import com.ww.mall.mvc.view.form.admin.SysDictForm;
import com.ww.mall.mvc.view.query.admin.SysDictQuery;
import com.ww.mall.mvc.view.vo.admin.SysDictVO;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:00
 */
public interface SysDictService extends IService<SysDictEntity> {

    /**
     * 获取指定字典类型记录分页
     *
     * @param pagination 分页
     * @param query      查询条件
     * @return PageInfo<SysDictVO>
     */
    PageInfo<SysDictVO> page(Pagination pagination, SysDictQuery query);

    /**
     * 根据字典类型名和字典key获取字典value
     *
     * @param type 字典类型
     * @param code 字典码
     * @return label
     */
    String getDictValue(String type, String code);

    /**
     * 根据字典类型获取字典
     *
     * @param dictType 字典类型
     * @return List<SysDictVO>
     */
    List<SysDictVO> getDict(String dictType);

    /**
     * 新增
     *
     * @param form 新增参数
     */
    void save(SysDictForm form);

    /**
     * 修改
     *
     * @param form 修改参数
     */
    void update(SysDictForm form);

    /**
     * 修改指定记录启用状态
     *
     * @param id 主键id
     */
    void updateStatus(Long id);

    /**
     * 批量删除
     *
     * @param ids id集合
     * @return BatchProcessingResult
     */
    BatchProcessingResult batchDelete(List<Long> ids);


}
