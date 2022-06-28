package com.example.demo.mqtt;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.es.ElasticsearchConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 接收处理MQTT消息，并保存到ES中
 */
@Component
@Log4j2
public class MqttReceivedMessageHandler {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @ServiceActivator(inputChannel = MqttConfig.CHANNEL_NAME_IN)
    public void handleMessage(Message<byte[]> message) throws MessagingException, IOException {
        // 主题
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
        // 数据内容
        String content = new String(message.getPayload());
        log.info("收到数据，主题：{}，内容：{}", topic, content);


        String[] topicParts = Arrays.stream(topic.split("/"))
                                    .map(StringUtils::trim)
                                    .filter(StringUtils::isNotBlank)
                                    .toArray(String[]::new);
        // 网关序列号
        String gatewayNo = topicParts[topicParts.length - 3];
        // 设备序列号
        String deviceNo = topicParts[topicParts.length - 2];

        JSONObject jsonObject = JSON.parseObject(content);
        // 产量数据
        long count = jsonObject.getJSONObject("params").getLongValue("count");

        Map<String, Object> data = new HashMap<>();
        data.put("gatewayNo", gatewayNo);
        data.put("deviceNo", deviceNo);
        data.put("count", count);
        data.put("createTime", new Date());
        // 保存数据到ES
        IndexResponse response = elasticsearchClient.index(request -> {
            request.index(ElasticsearchConfig.INDEX_NAME);
            request.document(data);
            return request;
        });
        if(!StringUtils.equalsIgnoreCase("created", response.result().jsonValue())) {
            log.error("保存数据失败，返回状态：{}", response.result().jsonValue());
        }
    }
}
