package com.ww.mall.mongodb.common;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * @author ww
 * @create 2024-08-23- 18:09
 * @description:
 */
@Data
public class BaseDoc implements Serializable {

    @Id
    private String id;

    private String createTime;

    private String updateTime;

}
