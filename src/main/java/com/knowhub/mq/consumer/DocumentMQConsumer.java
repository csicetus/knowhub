package com.knowhub.mq.consumer;

import com.knowhub.entity.Document;
import com.knowhub.mapper.DocumentMapper;
import com.knowhub.mq.DocumentVectorizeMessage;
import com.knowhub.mq.config.DocumentMQConfig;
import com.knowhub.service.DocumentService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentMQConsumer {

    private final DocumentMapper documentMapper;
    private final DocumentService documentService;

    @RabbitListener(queues = DocumentMQConfig.DOCUMENT_QUEUE)
    public void handleVectorize(DocumentVectorizeMessage message,
                                Channel channel,
                                Message amqpMessage) throws IOException {
        // 设置 SecurityContext，让多租户插件能取到 userId
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        message.getUserId(), null, List.of()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);

        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        log.info("收到向量化消息: documentId={}", message.getDocumentId());

        try {
            // 1. 从磁盘读取文件内容
            String content = Files.readString(Paths.get(message.getFilePath()));

            // 2. 调用向量化（复用 DocumentService 里的方法）
            // 这里直接调用 generateAndStoreEmbeddings
            // 注意：parseDocument 需要 MultipartFile，这里直接用文件内容
            documentService.vectorizeContent(content,
                    message.getDocumentId(), message.getKnowledgeBaseId(), message.getUserId());

            // 3. 手动 ACK，告诉 RabbitMQ 消息处理成功，可以删除
            channel.basicAck(deliveryTag, false);
            log.info("向量化完成: documentId={}", message.getDocumentId());
        } catch (Exception e) {
            log.error("向量化失败: documentId={}", message.getDocumentId(), e);

            // 更新文档状态为失败
            Document document = Document.builder()
                    .id(message.getDocumentId())
                    .status(3)
                    .errorMsg(e.getMessage())
                    .build();
            documentMapper.updateById(document);

            // 手动 NACK，requeue=false 表示不重新入队
            // 消息会根据死信配置转发到死信队列
            channel.basicNack(deliveryTag, false, false);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @RabbitListener(queues = DocumentMQConfig.DOCUMENT_DLQ)
    public void handleDeadLetter(DocumentVectorizeMessage message,
                                 Channel channel,
                                 Message amqpMessage) throws IOException {
        log.error("文档向量化进入死信队列，需要人工处理: documentId={}, filePath={}",
                message.getDocumentId(), message.getFilePath());
        channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
    }
}
