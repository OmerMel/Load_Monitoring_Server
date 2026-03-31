package com.finalproject.load_monitoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.service.OccupancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MqttConfig {

    private final OccupancyService occupancyService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // The client factory for the MQTT topic
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setCleanSession(false); // false to keep the session alive
        factory.setConnectionOptions(options);
        return factory;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // The channel for the MQTT topic
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // The listener for the MQTT topic
    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory(), topic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // The handler for the MQTT message
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            String payload = (String) message.getPayload();
            try {
                // Convert JSON string (that comes from the MQTT topic) to DTO
                SensorDataDTO sensorData = objectMapper.readValue(payload, SensorDataDTO.class);
                
                // Log the received data
                log.info("MQTT Message Received: Train ID: {}, Carriage: {}, Camera: {}, IR: {}", 
                        sensorData.getTrainId(), 
                        sensorData.getCarriageNumber(),
                        sensorData.getCameraCount(),
                        sensorData.getIrCount());
                
                // Call the service method for data fusion
                occupancyService.updateOccupancy(sensorData);
                
            } catch (Exception e) {
                // Error handling for invalid JSON
                log.error("Failed to process MQTT message: {}", e.getMessage());
            }
        };
    }
}
