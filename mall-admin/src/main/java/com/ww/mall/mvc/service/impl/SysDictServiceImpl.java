package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.Cache;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.common.utils.BeanCopierUtils;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysDictDao;
import com.ww.mall.mvc.entity.SysDictEntity;
import com.ww.mall.mvc.service.SysDictService;
import com.ww.mall.mvc.view.form.admin.SysDictForm;
import com.ww.mall.mvc.view.query.admin.SysDictQuery;
import com.ww.mall.mvc.view.vo.admin.SysDictVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:07
 */
@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictDao, SysDictEntity> implements SysDictService {

    @Override
    public PageInfo<SysDictVO> page(Pagination pagination, SysDictQuery query) {
        MyPageHelper.startPage(pagination, SysDictVO.class);
        List<SysDictEntity> entityList = baseMapper.page(query);
        return new MyPageInfo<>(entityList).convert(entity -> {
            SysDictVO vo = new SysDictVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    @Override
    @Cache(key = RedisKeyConstant.DICT_KEY)
    public String getDictValue(String type, String code) {
        return baseMapper.getDictValue(type, code);
    }

    @Override
    public List<SysDictVO> getDict(String dictType) {
        return baseMapper.getDict(dictType);
    }

    @Override
    public void save(SysDictForm form) {
        validRecord(form.getDictType(), form.getCode());
        SysDictEntity entity = new SysDictEntity();
        BeanCopierUtils.copyProperties(form, entity);
        super.save(entity);
    }

    @Override
    public void update(SysDictForm form) {
        validRecord(form.getDictType(), form.getCode());
        SysDictEntity entity = new SysDictEntity();
        BeanCopierUtils.copyProperties(form, entity);
        super.updateById(entity);
    }

    @Override
    public void updateStatus(Long id) {
        //根据主键获取记录判断记录是否存在
        SysDictEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("id：【" + id + "】记录不存在");
        }
        //根据原纪录状态修改当前记录状态
        boolean status = true;
        if (Boolean.TRUE.equals(entity.getStatus())) {
            status = false;
        }
        SysDictEntity newEntity = new SysDictEntity();
        newEntity.setId(id);
        newEntity.setStatus(status);
        super.updateById(newEntity);
    }

    @Override
    public BatchProcessingResult batchDelete(List<Long> ids) {
        if (ids.isEmpty()) {
            throw new ValidatorException("至少选中一条记录删除");
        }
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

    /**
     * 字典类型和字典码进行查重
     *
     * @param type type
     * @param code code
     */
    private void validRecord(String type, String code) {
        //字典类型和字典码进行查重
        SysDictEntity dict = super.getOne(new QueryWrapper<SysDictEntity>()
                .eq("`dict_type`", type)
                .eq("code", code));
        if (dict != null) {
            throw new ValidatorException("当前记录已存在");
        }
    }

}
