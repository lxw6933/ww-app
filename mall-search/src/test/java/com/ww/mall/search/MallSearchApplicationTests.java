package com.ww.mall.search;

import com.alibaba.fastjson.JSON;
import com.ww.mall.search.config.MallElasticSearchConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
        request.id("1");
        Demo demo = new Demo("ww",25);
        request.source(JSON.toJSONString(demo), XContentType.JSON);
        IndexResponse response = client.index(request, MallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(response);
    }

    @Test
    void searchData() throws IOException {
        SearchRequest request = new SearchRequest();
        // 检索哪个索引
        request.indices("demo");
        // 设置检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("name", "ww"));

        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("age-agg-name").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);
        System.out.println(sourceBuilder);

        request.source(sourceBuilder);
        // 执行检索
        SearchResponse response = client.search(request, MallElasticSearchConfig.COMMON_OPTIONS);
        // 检索结果
        // 检索总结果数量
        long total = response.getHits().getTotalHits().value;
        System.out.println(total);
        System.out.println(response.toString());
        // 获取所有检索到的数据
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            System.out.println("***************begin**************");
            System.out.println(hit.getIndex());
            System.out.println(hit.getId());
            System.out.println(hit.getSourceAsString());
//            Demo demo = JSON.parseObject(hit.getSourceAsString(), Demo.class);
//            System.out.println(demo.toString());
            System.out.println("***************end**************");
        }
        Aggregations aggregations = response.getAggregations();
        aggregations.asList().forEach(res -> {
            String name = res.getName();

        });

    }


}
