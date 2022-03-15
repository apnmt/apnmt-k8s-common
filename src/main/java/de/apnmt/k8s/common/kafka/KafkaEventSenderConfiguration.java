package de.apnmt.k8s.common.kafka;

import brave.kafka.interceptor.TracingConsumerInterceptor;
import brave.kafka.interceptor.TracingProducerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaEventSenderConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.zipkin.base-url}")
    private String zipkinBaseUrl;

    @Value("${spring.zipkin.enabled}")
    private boolean zipkinEnabled;

    private Map<String, Object> producerConfigs() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        if (zipkinEnabled) {
            props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class);
            props.put("zipkin.http.endpoint", zipkinBaseUrl + "/api/v2/spans");
            props.put("zipkin.sender.type", "HTTP");
            props.put("zipkin.encoding", "JSON");
            props.put("zipkin.remote.service.name", "kafka");
            props.put("zipkin.local.service.name", appName);
            props.put("zipkin.trace.id.128bit.enabled", "true");
            props.put("zipkin.sampler.rate", "1.0F");
        }
        return props;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(this.producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(this.producerFactory());
    }

}
