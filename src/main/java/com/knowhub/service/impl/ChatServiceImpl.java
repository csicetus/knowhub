package com.knowhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowhub.common.exception.BusinessException;
import com.knowhub.dto.ChatRequest;
import com.knowhub.entity.Conversation;
import com.knowhub.entity.Message;
import com.knowhub.mapper.ConversationMapper;
import com.knowhub.mapper.MessageMapper;
import com.knowhub.service.ChatService;
import com.knowhub.service.KnowledgeBaseValidator;
import com.knowhub.util.RagPromptBuilder;
import com.knowhub.vo.ChatMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String MESSAGE_ROLE_ASSISTANT = "assistant";
    private static final String MESSAGE_ROLE_USER = "user";
    private static final Integer CHAT_HISTORY_LIMIT = 5;

    private final ConversationMapper conversationMapper;
    private final ChatClient chatClient;  // Spring AI 的 LLM 调用客户端
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final KnowledgeBaseValidator knowledgeBaseValidator;
    private final MessageMapper messageMapper;
    private final RagPromptBuilder ragPromptBuilder;

    // 主线程立即返回 SseEmitter，建立 SSE 连接
    // 异步线程做耗时的 LLM 调用，每生成一段通过 SSE 推送
    // SecurityContext 必须手动传递，ThreadLocal 线程隔离
    @Override
    public SseEmitter handleQuestion(ChatRequest request, Long userId) {
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        knowledgeBaseValidator.validateAndGet(knowledgeBaseId, userId);

        final Long finalConversationId;
        String title = request.getQuestion().length() > 50
                ? request.getQuestion().substring(0, 50)
                : request.getQuestion();

        if (request.getConversationId() == null) {
            Conversation conversation = Conversation.builder()
                    .userId(userId)
                    .knowledgeBaseId(knowledgeBaseId)
                    .title(title)
                    .build();
            conversationMapper.insert(conversation);
            finalConversationId = conversation.getId();
        } else {
            validateConversation(userId, request.getConversationId());
            finalConversationId = request.getConversationId();
        }

        Message message = Message.builder()
                .conversationId(finalConversationId)
                .userId(userId)
                .role(MESSAGE_ROLE_USER)
                .content(request.getQuestion())
                .tokensUsed(0)
                .build();
        messageMapper.insert(message);

        // 创建 SseEmitter，超时时间 3 分钟
        SseEmitter sseEmitter = new SseEmitter(180_000L);

        // 获取当前线程的 SecurityContext
        SecurityContext securityContext = SecurityContextHolder.getContext();

        executor.submit(() -> {
            // 把 SecurityContext 设置到异步线程里
            SecurityContextHolder.setContext(securityContext);
            try {
                // 1. 查找最近 5 条历史消息
                StringBuilder recentChatHistory = new StringBuilder();
                List<Message> messageList = getRecentChatHistory(finalConversationId);
                for (int i = messageList.size() - 1; i >= 0; i--) {
                    Message msg = messageList.get(i);
                    if (msg.getRole().equals(MESSAGE_ROLE_USER)) {
                        recentChatHistory.append("User: ").append(msg.getContent()).append("\n");
                    } else if (msg.getRole().equals(MESSAGE_ROLE_ASSISTANT)) {
                        recentChatHistory.append("Assistant: ").append(msg.getContent()).append("\n");
                    }
                }

                // 2. 调用 LLM 流式生成答案，同时通过 SSE 推送
                StringBuilder fullAnswer = new StringBuilder();

                chatClient.prompt(ragPromptBuilder.buildPrompt(knowledgeBaseId,
                                userId, request.getQuestion(), recentChatHistory))
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            fullAnswer.append(chunk);
                            try {
                                ChatMessageVO chatMessageVO = new ChatMessageVO();
                                chatMessageVO.setContent(chunk);
                                chatMessageVO.setDone(false);
                                chatMessageVO.setConversationId(finalConversationId);

                                sseEmitter.send(chatMessageVO);
                            } catch (IOException e) {
                                log.error("SSE 推送失败: {}", e.getMessage());
                            }
                        })
                        .blockLast();

                // 3. 保存 assistant 消息到数据库
                Message assistantMessage = Message.builder()
                        .conversationId(finalConversationId)
                        .userId(userId)
                        .role(MESSAGE_ROLE_ASSISTANT)
                        .content(fullAnswer.toString())
                        .tokensUsed(0)
                        .build();
                messageMapper.insert(assistantMessage);

                // 4. 发送结束信号
                ChatMessageVO doneVO = new ChatMessageVO();
                doneVO.setContent("");
                doneVO.setDone(true);
                doneVO.setConversationId(finalConversationId);
                sseEmitter.send(doneVO);
                sseEmitter.complete();
            } catch (Exception e) {
                log.error("RAG 处理失败", e);
                sseEmitter.completeWithError(e);
            } finally {
                // 清理，防止内存泄露
                SecurityContextHolder.clearContext();
            }
        });

        return sseEmitter;
    }

    @Override
    public String askQuestion(Long knowledgeBaseId, Long userId, String question) {
        String prompt = ragPromptBuilder.buildPrompt(knowledgeBaseId, userId, question, new StringBuilder());
        return chatClient.prompt(prompt).call().content();
    }

    private void validateConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(404, "对话不存在或无权限");
        }
    }

    @Override
    public List<ChatMessageVO> getChatHistory(Long conversationId, Long userId) {
        validateConversation(userId, conversationId);

        List<Message> messages = messageMapper.selectList(
          new LambdaQueryWrapper<Message>()
                  .eq(Message::getConversationId, conversationId)
                  .orderByAsc(Message::getCreatedTime)
        );

        return messages.stream()
                .map(msg -> {
                    ChatMessageVO vo = new ChatMessageVO();
                    vo.setConversationId(msg.getConversationId());
                    vo.setContent(msg.getContent());
                    vo.setDone(true);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // 倒序返回前 n 条 message
    private List<Message> getRecentChatHistory(Long conversationId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByDesc(Message::getCreatedTime)
                        .last("LIMIT " + CHAT_HISTORY_LIMIT)
        );
    }
}
