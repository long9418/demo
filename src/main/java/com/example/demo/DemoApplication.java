package com.example.demo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@SpringBootApplication
@RestController
public class DemoApplication {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    public static void main(String[] args) {
         SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/query")
    public Object query(@RequestParam(defaultValue = "D00001") String deviceNo) throws IOException {
        SearchResponse<Map> searchResponse = elasticsearchClient.search(request -> {
            request.index("test_index");
            request.size(10);
            request.sort(sort -> sort.field(f -> f.field("createTime").order(SortOrder.Desc)));
            request.query(query ->
                    query.bool(bool -> bool
                            .must(must -> must.term(term -> term.field("deviceNo.keyword").value(deviceNo)))
                            .must(must -> must.range(range -> range.field("createTime").gte(JsonData.of(new Date(0)))))));
            return request;
        }, Map.class);

        List<Map> list = searchResponse.hits().hits().stream().map(hit -> hit.source()).collect(Collectors.toList());
        return list;
    }
}
