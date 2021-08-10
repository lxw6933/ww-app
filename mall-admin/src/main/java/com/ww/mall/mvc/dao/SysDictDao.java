package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysDictEntity;
import com.ww.mall.mvc.view.query.admin.SysDictQuery;
import com.ww.mall.mvc.view.vo.admin.SysDictVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:07
 */
@Mapper
public interface SysDictDao extends BaseMapper<SysDictEntity> {

    /**
     * 获取指定字典类型记录分页
     *
     * @param query query
     * @return List<SysDictEntity>
     */
    List<SysDictEntity> page(SysDictQuery query);

    /**
     * 根据字典类型名和字典key获取字典value
     *
     * @param type 字典类型
     * @param code 字典码
     * @return label
     */
    String getDictValue(@Param("type") String type, @Param("code") String code);

    /**
     * 根据字典类型获取字典
     *
     * @param dictType 字典类型
     * @return List<SysDictVO>
     */
    List<SysDictVO> getDict(String dictType);

}
