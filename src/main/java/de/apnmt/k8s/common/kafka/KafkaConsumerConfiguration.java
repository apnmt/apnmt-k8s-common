package de.apnmt.k8s.common.kafka;

import brave.kafka.interceptor.TracingConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-startup}")
    private boolean autoStartup;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.zipkin.base-url}")
    private String zipkinBaseUrl;

    @Value("${spring.zipkin.enabled}")
    private boolean zipkinEnabled;

    private Map<String, Object> consumerConfigs() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        if (zipkinEnabled) {
            props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
            props.put("zipkin.http.endpoint", zipkinBaseUrl + "/api/v2/spans");
            props.put("zipkin.sender.type", "HTTP");
            props.put("zipkin.encoding", "JSON");
            props.put("zipkin.remote.service.name", "kafka");
            props.put("zipkin.local.service.name", appName);
            props.put("zipkin.trace.id.128bit.enabled", "true");
            props.put("zipkin.sampler.rate", "1.0F");
        }

        // allows a pool of processes to divide the work of consuming and processing records
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(this.consumerConfigs());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(this.consumerFactory());
        factory.setAutoStartup(autoStartup);
        return factory;
    }

}
