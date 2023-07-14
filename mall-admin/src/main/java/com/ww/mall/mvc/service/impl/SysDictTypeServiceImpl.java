package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysDictTypeDao;
import com.ww.mall.mvc.entity.SysDictTypeEntity;
import com.ww.mall.mvc.service.SysDictService;
import com.ww.mall.mvc.service.SysDictTypeService;
import com.ww.mall.mvc.view.form.admin.SysDictTypeForm;
import com.ww.mall.mvc.view.vo.admin.SysDictTypeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:49
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeDao, SysDictTypeEntity> implements SysDictTypeService {

    @Resource
    private SysDictService sysDictService;

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    @Override
    public PageInfo<SysDictTypeVO> page(Pagination pagination, QueryWrapper<SysDictTypeEntity> query) {
        MyPageHelper.startPage(pagination, SysDictTypeVO.class);
        List<SysDictTypeEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysDictTypeVO vo = new SysDictTypeVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysDictTypeVO
     */
    @Override
    public SysDictTypeVO info(Long id) {
        SysDictTypeEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("id：【" + id + "】记录不存在");
        }
        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增
     *
     * @param form 表单信息
     */
    @Override
    public void save(SysDictTypeForm form) {
        SysDictTypeEntity entity = new SysDictTypeEntity();
        BeanUtils.copyProperties(form, entity);
        super.save(entity);
    }

    /**
     * 编辑
     *
     * @param form 表单信息
     */
    @Override
    public void update(SysDictTypeForm form) {
        SysDictTypeEntity entity = new SysDictTypeEntity();
        BeanUtils.copyProperties(form, entity);
        super.updateById(entity);
    }

    @Override
    public void updateStatus(Long id) {
        //根据主键获取记录判断记录是否存在
        SysDictTypeEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("id：【" + id + "】记录不存在");
        }
        //根据原纪录状态修改当前记录状态
        boolean status = true;
        if (Boolean.TRUE.equals(entity.getStatus())) {
            status = false;
        }
        SysDictTypeEntity newEntity = new SysDictTypeEntity();
        newEntity.setId(id);
        newEntity.setStatus(status);
        super.updateById(newEntity);
    }

    /**
     * 批量删除
     *
     * @param ids 主键ID
     */
    @Override
    public BatchProcessingResult batchDelete(List<Long> ids) {
        BatchProcessingResult result = new BatchProcessingResult();
        for (Long id : ids) {
            try {
                super.removeById(id);
                result.addSuccess(1);
            } catch (Exception ex) {
                result.addError(1).addErrorMsg(ex.getMessage());
            }
        }
        return result;
    }
}
