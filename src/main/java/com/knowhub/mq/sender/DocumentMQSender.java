package com.knowhub.mq.sender;

import com.knowhub.mq.DocumentVectorizeMessage;
import com.knowhub.mq.config.DocumentMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentMQSender {

    private final RabbitTemplate rabbitTemplate;

    public void sendVectorizeMessage(DocumentVectorizeMessage message) {
        rabbitTemplate.convertAndSend(
                DocumentMQConfig.DOCUMENT_EXCHANGE,
                DocumentMQConfig.ROUTING_KEY,
                message
        );
        log.info("发送向量化消息: documentId={}", message.getDocumentId());
    }
}
