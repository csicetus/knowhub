package com.knowhub.controller;

import com.knowhub.aspect.RateLimit;
import com.knowhub.dto.ChatRequest;
import com.knowhub.service.ChatService;
import com.knowhub.util.SecurityUtil;
import com.knowhub.vo.ChatMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @RateLimit(limit = 5, key = "chat")
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startChat(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.handleQuestion(chatRequest, SecurityUtil.getCurrentUserId());
    }

    @GetMapping(value = "/history")
    public List<ChatMessageVO> getChatHistory(@RequestParam Long conversationId) {
        return chatService.getChatHistory(conversationId, SecurityUtil.getCurrentUserId());
    }
}
