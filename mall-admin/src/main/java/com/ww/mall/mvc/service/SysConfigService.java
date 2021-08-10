package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.entity.SysConfigEntity;
import com.ww.mall.mvc.view.form.admin.SysConfigForm;
import com.ww.mall.mvc.view.vo.admin.SysConfigVO;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 19:54
 */
public interface SysConfigService extends IService<SysConfigEntity> {

    /**
     * 系统配置记录分页
     *
     * @param pagination 分页
     * @param query      查询条件
     * @return PageInfo<SysConfigVO>
     */
    PageInfo<SysConfigVO> page(Pagination pagination, QueryWrapper<SysConfigEntity> query);

    /**
     * 根据key获取value
     *
     * @param key key
     * @return value
     */
    String getConfigValue(String key);

    /**
     * 新增
     *
     * @param form 新增参数
     */
    void save(SysConfigForm form);

    /**
     * 修改
     *
     * @param form 修改参数
     */
    void update(SysConfigForm form);

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

    /**
     * 根据key，获取value的Object对象
     *
     * @param key   key
     * @param clazz Object对象
     * @return 配置对象类
     */
    <T> T getConfigObject(String key, Class<T> clazz);

}
