package com.knowhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotNull
    private Long knowledgeBaseId;

    @NotBlank
    private String question;

    private Long conversationId;
}
