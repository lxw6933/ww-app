package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2021-04-16 09:34
 */
@Data
@TableName("user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    @TableId
    private Long id;
    @NotNull(message = "用户名称不能为空")
    private String name;
    private Integer sex;
    private Double salary;
    private Integer age;
    private String email;
    @TableLogic
    private Integer deleted;

    /**
     * 注解@JsonFormat主要是后台到前台的时间格式的转换
     * 注解@DateTimeFormat主要是前后到后台的时间格式的转换
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT-8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
}
