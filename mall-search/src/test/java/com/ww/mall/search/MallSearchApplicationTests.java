package com.ww.mall.search;

import com.alibaba.fastjson.JSON;
import com.ww.mall.search.config.MallElasticSearchConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class MallSearchApplicationTests {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class Demo{
        private String name;
        private Integer age;
    }

    @Autowired
    MallElasticSearchConfig mallElasticSearchConfig;
    @Qualifier("restHighLevelClient")
    @Autowired
    RestHighLevelClient client;

    @Test
    void contextLoads() {
        System.out.println("===="+mallElasticSearchConfig);
    }

    @Test
    void testIndex() throws IOException {
        IndexRequest request = new IndexRequest("demo");
        Demo demo = new Demo("ww",18);
        request.source(JSON.toJSONString(demo), XContentType.JSON);
        IndexResponse response = client.index(request, MallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(response);
    }


}
