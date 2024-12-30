package com.ww.app.product.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@Data
@Document("t_spu_comment_replay")
public class SpuCommentReplay implements Serializable {

    @Id
    private String id;

    /**
     * 评论id
     */
    private Long commentId;

    /**
     * 回复id
     */
    private Long replyId;

}
