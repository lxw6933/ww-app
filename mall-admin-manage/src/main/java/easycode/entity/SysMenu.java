package easycode.entity;

import java.util.Date;

import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.io.Serializable;

import com.ww.mall.web.cmmon.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Data
@TableName("sys_menu")
@EqualsAndHashCode(callSuper = true)
public class SysMenu extends BaseEntity {
    /**
     * 物理主键
     */
    private Long id;

    /**
     * 平台
     */
    private String platform;

    /**
     * 类型
     */
    private String type;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 父级编号
     */
    private Long pid;

    /**
     * URL地址
     */
    private String url;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 是否有效
     */
    private Integer valid;

    /**
     * 平台id
     */
    private Long platformId;

    private Long version;

    private Long creatorId;

    private Long updaterId;

    private Date createTime;

    private Date updateTime;

}

