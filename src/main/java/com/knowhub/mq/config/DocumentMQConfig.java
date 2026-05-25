package com.knowhub.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentMQConfig {

    public static final String DOCUMENT_QUEUE = "document.vectorize.queue";
    public static final String DOCUMENT_DLQ = "document.vectorize.dlq";
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DOCUMENT_DLX = "document.dlx";
    public static final String ROUTING_KEY = "document.vectorize";
    public static final String DLQ_ROUTING_KEY = "document.dead";

    // 正常队列，绑定死信交换机
    // 消息被 nack 且 requeue=false 时，转发到死信交换机
    @Bean
    public Queue documentQueue() {
        return QueueBuilder.durable(DOCUMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DOCUMENT_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // 死信队列，处理失败超过3次的消息
    @Bean
    public Queue documentDeadLetterQueue() {
        return QueueBuilder.durable(DOCUMENT_DLQ).build();
    }

    // 正常交换机
    @Bean
    public DirectExchange documentExchange() { return new DirectExchange(DOCUMENT_EXCHANGE); }

    // 死信交换机
    @Bean
    public DirectExchange documentDeadLetterExchange() { return new DirectExchange(DOCUMENT_DLX); }

    // 正常队列绑定到正常交换机
    @Bean
    public Binding documentBinding() {
        return BindingBuilder
                .bind(documentQueue())
                .to(documentExchange())
                .with(ROUTING_KEY);
    }

    // 死信队列绑定到死信交换机
    @Bean
    public Binding documentDeadLetterBinding() {
        return BindingBuilder
                .bind(documentDeadLetterQueue())
                .to(documentDeadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }
}
