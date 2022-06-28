package com.example.demo.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 发送MQTT消息
 */
@Component
@MessagingGateway(defaultRequestChannel = MqttConfig.CHANNEL_NAME_OUT)
public interface MqttSendChannel {
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, byte[] data);

    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, byte[] data,
                    @Header(MqttHeaders.RETAINED) boolean retained);

    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, byte[] data,
                    @Header(MqttHeaders.RETAINED) boolean retained,
                    @Header(MqttHeaders.DUPLICATE) boolean duplicate);
}
