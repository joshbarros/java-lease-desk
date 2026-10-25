package com.leasedesk.unit;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic ownersTopic() {
        return TopicBuilder.name(Channels.OWNERS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic buildingsTopic() {
        return TopicBuilder.name(Channels.BUILDINGS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic tenantsTopic() {
        return TopicBuilder.name(Channels.TENANTS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic floorsTopic() {
        return TopicBuilder.name(Channels.FLOORS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic unitsTopic() {
        return TopicBuilder.name(Channels.UNITS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic applicationsTopic() {
        return TopicBuilder.name(Channels.APPLICATIONS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic quotesTopic() {
        return TopicBuilder.name(Channels.QUOTES).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic vetsTopic() {
        return TopicBuilder.name(Channels.VETS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic holdsTopic() {
        return TopicBuilder.name(Channels.HOLDS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic deedsTopic() {
        return TopicBuilder.name(Channels.DEEDS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic invoicesTopic() {
        return TopicBuilder.name(Channels.INVOICES).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic signsTopic() {
        return TopicBuilder.name(Channels.SIGNS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic vacatesTopic() {
        return TopicBuilder.name(Channels.VACATES).partitions(1).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        JsonSerializer<Object> values = new JsonSerializer<>(objectMapper);
        values.setAddTypeInfo(true);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), values);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
        JsonDeserializer<Object> values = new JsonDeserializer<>(objectMapper);
        values.addTrustedPackages("*");
        values.setUseTypeHeaders(true);
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), values);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }
}
