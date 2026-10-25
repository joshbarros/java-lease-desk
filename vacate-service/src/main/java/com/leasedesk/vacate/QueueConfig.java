package com.leasedesk.vacate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class QueueConfig {

    @Bean
    public Queue rateDlq() {
        return QueueBuilder.durable(Channels.RATE_DLQ).build();
    }

    @Bean
    @DependsOn("rateDlq")
    public Queue rateQueue() {
        return QueueBuilder.durable(Channels.RATE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RATE_DLQ)
                .build();
    }

    @Bean
    public Queue reserveDlq() {
        return QueueBuilder.durable(Channels.RESERVE_DLQ).build();
    }

    @Bean
    @DependsOn("reserveDlq")
    public Queue reserveQueue() {
        return QueueBuilder.durable(Channels.RESERVE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RESERVE_DLQ)
                .build();
    }

    @Bean
    public Queue decideDlq() {
        return QueueBuilder.durable(Channels.DECIDE_DLQ).build();
    }

    @Bean
    @DependsOn("decideDlq")
    public Queue decideQueue() {
        return QueueBuilder.durable(Channels.DECIDE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.DECIDE_DLQ)
                .build();
    }

    @Bean
    public Queue vacateDlq() {
        return QueueBuilder.durable(Channels.VACATE_DLQ).build();
    }

    @Bean
    @DependsOn("vacateDlq")
    public Queue vacateQueue() {
        return QueueBuilder.durable(Channels.VACATE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.VACATE_DLQ)
                .build();
    }

    @Bean
    public Queue vacateUnitDlq() {
        return QueueBuilder.durable(Channels.RELEASE_UNIT_DLQ).build();
    }

    @Bean
    @DependsOn("vacateUnitDlq")
    public Queue vacateUnitQueue() {
        return QueueBuilder.durable(Channels.RELEASE_UNIT)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RELEASE_UNIT_DLQ)
                .build();
    }

    @Bean
    public Queue deedDlq() {
        return QueueBuilder.durable(Channels.DEED_DLQ).build();
    }

    @Bean
    @DependsOn("deedDlq")
    public Queue deedQueue() {
        return QueueBuilder.durable(Channels.DEED)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.DEED_DLQ)
                .build();
    }

    @Bean
    public Queue keepDlq() {
        return QueueBuilder.durable(Channels.KEEP_DLQ).build();
    }

    @Bean
    @DependsOn("keepDlq")
    public Queue keepQueue() {
        return QueueBuilder.durable(Channels.KEEP)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.KEEP_DLQ)
                .build();
    }

    @Bean
    public Queue chargeDlq() {
        return QueueBuilder.durable(Channels.CHARGE_DLQ).build();
    }

    @Bean
    @DependsOn("chargeDlq")
    public Queue chargeQueue() {
        return QueueBuilder.durable(Channels.CHARGE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.CHARGE_DLQ)
                .build();
    }

    @Bean
    public Queue signDlq() {
        return QueueBuilder.durable(Channels.SIGN_DLQ).build();
    }

    @Bean
    @DependsOn("signDlq")
    public Queue signQueue() {
        return QueueBuilder.durable(Channels.SIGN)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.SIGN_DLQ)
                .build();
    }

    @Bean
    public Queue cancelDlq() {
        return QueueBuilder.durable(Channels.BREAK_DLQ).build();
    }

    @Bean
    @DependsOn("cancelDlq")
    public Queue cancelQueue() {
        return QueueBuilder.durable(Channels.BREAK)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.BREAK_DLQ)
                .build();
    }

    @Bean
    public Queue restoreDlq() {
        return QueueBuilder.durable(Channels.RESTORE_DLQ).build();
    }

    @Bean
    @DependsOn("restoreDlq")
    public Queue restoreQueue() {
        return QueueBuilder.durable(Channels.RESTORE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RESTORE_DLQ)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
