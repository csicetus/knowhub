package com.knowhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotNull
    private Long knowledgeBaseId;

    @NotBlank
    @Size(max = 2000, message = "问题长度不能超过2000字")
    private String question;

    private Long conversationId;
}
