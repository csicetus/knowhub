package com.knowhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long userId;

    private String role;

    private String content;

    private Integer tokensUsed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
