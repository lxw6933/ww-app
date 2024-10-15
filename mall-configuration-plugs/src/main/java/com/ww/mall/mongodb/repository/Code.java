package com.ww.mall.mongodb.repository;

import com.ww.mall.mongodb.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-10-15- 15:00
 * @description:
 */
@Data
@Document("t_codes")
@EqualsAndHashCode(callSuper = true)
public class Code extends BaseDoc {

    private String batchNo;

    private String code;
}
