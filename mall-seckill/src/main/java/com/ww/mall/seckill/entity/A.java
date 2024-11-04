package com.ww.mall.seckill.entity;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-11-04 22:56
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@AutoMapper(target = B.class)
public class A {

    private String username;
    private int age;
    private boolean young;

}
