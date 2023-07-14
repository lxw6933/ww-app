package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysLogDao;
import com.ww.mall.mvc.entity.SysLogEntity;
import com.ww.mall.mvc.entity.dto.SysLogDTO;
import com.ww.mall.mvc.service.SysLogService;
import com.ww.mall.mvc.view.query.admin.SysLogQuery;
import com.ww.mall.mvc.view.vo.admin.SysLogVO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-13 09:25
 */
@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogDao, SysLogEntity> implements SysLogService {

    @Override
    public PageInfo<SysLogVO> page(Pagination pagination, SysLogQuery query) {
        MyPageHelper.startPage(pagination, SysLogVO.class);
        List<SysLogDTO> dtoList = baseMapper.page(query);
        return new MyPageInfo<>(dtoList).convert(
                res -> {
                    SysLogVO vo = new SysLogVO();
                    BeanCopierUtils.copyProperties(res, vo);
                    return vo;
                }
        );
    }

    @Override
    public SysLogVO info(Long id) {
        SysLogDTO dto = baseMapper.info(id);
        if (dto == null) {
            throw new ValidatorException("admin.currentRecord.null");
        }
        SysLogVO vo = new SysLogVO();
        BeanCopierUtils.copyProperties(dto, vo);
        return vo;
    }

    @Override
    public ByteArrayOutputStream exportExcel(SysLogQuery query) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<SysLogDTO> list = baseMapper.exportExcel(query);
        // TODO: 2021/5/13 导出excel 
        return out;
    }
}
