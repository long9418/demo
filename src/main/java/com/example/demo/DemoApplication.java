package com.example.demo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.example.demo.es.ElasticsearchConfig;
import com.example.demo.mqtt.MqttSendChannel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
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
    @Autowired
    private MqttSendChannel mqttSendChannel;

    public static void main(String[] args) {
         SpringApplication.run(DemoApplication.class, args);
    }

    /** 发送数据到MQTT **/
    @GetMapping("/sendData")
    public Object sendData() {
        Map<String, Object> data = new HashMap<>();
        data.put("time", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        data.put("params", new HashMap() {
            {
                put("count", RandomUtils.nextInt(1, 10));
            }
        });
        mqttSendChannel.sendToMqtt("demo/G00001/D00001/data", JSON.toJSONString(data).getBytes());
        return "ok";
    }

    /** 从ES中查询数据 **/
    @GetMapping("/query")
    public Object query(@RequestParam(defaultValue = "D00001") String deviceNo) throws IOException {
        // 查询数据
        SearchResponse<Map> searchResponse = elasticsearchClient.search(request -> {
            request.allowNoIndices(true);
            request.ignoreUnavailable(true);
            // 索引名称
            request.index(ElasticsearchConfig.INDEX_NAME);
            // 分页，第页大小
            request.size(10);
            // 分页，偏移量
            request.from(0);
            // 排序
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
