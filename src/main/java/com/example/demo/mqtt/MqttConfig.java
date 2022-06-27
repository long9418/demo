package com.example.demo.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@IntegrationComponentScan
public class MqttConfig {
    public static final String CHANNEL_NAME_IN = "mqttInboundChannel";
    public static final String CHANNEL_NAME_OUT = "mqttOutboundChannel";

    @Value("${youngsun.mqtt.username}")
    private String username;

    @Value("${youngsun.mqtt.password}")
    private String password;

    @Value("${youngsun.mqtt.url}")
    private String hostUrl;

    @Value("#{'${youngsun.mqtt.client.id}'.concat('_').concat('${spring.profiles.active}').concat('_').concat((T(java.lang.Math).random() * 10000).intValue())}")
    private String clientId;

    @Value("${youngsun.mqtt.default.receiveTopic}")
    private String[] receiveTopic;

    @Value("${youngsun.mqtt.completionTimeout}")
    private int completionTimeout;   //连接超时

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setServerURIs(new String[]{hostUrl});
        mqttConnectOptions.setKeepAliveInterval(10);
        mqttConnectOptions.setMaxInflight(2000);
        mqttConnectOptions.setCleanSession(true);
        return mqttConnectOptions;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions mqttConnectOptions) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions());
        return factory;
    }

    //接收通道
    @Bean(CHANNEL_NAME_IN)
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean(CHANNEL_NAME_OUT)
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息处理器（生产者）
     * @return {@link MessageHandler}
     */
    @Bean
    @ServiceActivator(inputChannel = CHANNEL_NAME_OUT)
    public MessageHandler mqttOutbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId + "_outbound", mqttClientFactory(null));
        messageHandler.setAsync(true);
        // messageHandler.setDefaultTopic();
        // 设置转换器，发送bytes
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        messageHandler.setConverter(converter);
        return messageHandler;
    }


    /**
     * MQTT消息订阅绑定（消费者）
     * @return {@link MessageProducer}
     */
    @Bean
    public MessageProducer inbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_inbound", mqttClientFactory(null), receiveTopic);
        adapter.setCompletionTimeout(completionTimeout);
        // 设置转换器，接收bytes
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }
}
