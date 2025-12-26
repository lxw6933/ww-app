package com.ww.app.open.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放平台配置实体
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 开放平台系统配置，支持动态配置和扩展
 */
@Data
@TableName("open_config")
@EqualsAndHashCode(callSuper = true)
public class OpenConfig extends BaseEntity {

    /**
     * 配置键（唯一标识）
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 配置类型：0-系统配置，1-业务配置，2-扩展配置
     */
    private Integer configType;

    /**
     * 配置分组
     */
    private String configGroup;

    /**
     * 是否可修改：0-否，1-是
     */
    private Integer editable;

    /**
     * 生效环境：all-所有环境，dev-开发，test-测试，prod-生产
     */
    private String environment;
}


