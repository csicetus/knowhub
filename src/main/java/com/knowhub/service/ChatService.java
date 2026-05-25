package com.knowhub.service;

import com.knowhub.dto.ChatRequest;
import com.knowhub.vo.ChatMessageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {
    SseEmitter handleQuestion(ChatRequest request, Long userId);
    String askQuestion(Long knowledgeBaseId, Long userId, String question);
    List<ChatMessageVO> getChatHistory(Long conversationId, Long userId);
}
