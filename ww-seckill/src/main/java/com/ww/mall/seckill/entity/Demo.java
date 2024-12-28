package com.ww.mall.seckill.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author ww
 * @create 2024-06-01 14:12
 * @description:
 */
@Data
@Document("demo")
public class Demo {
    @Id
    private String id;
    private Integer empNo;
    private Integer salary;
    private Date fromDate;
    private Date toDate;
}
