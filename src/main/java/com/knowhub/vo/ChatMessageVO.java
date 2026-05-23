package com.knowhub.vo;

import lombok.Data;

@Data
public class ChatMessageVO {

    private String content;

    private Boolean done;

    private Long conversationId;
}
