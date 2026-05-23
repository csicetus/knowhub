package com.knowhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {

    private Long id;

    private Long userId;

    private String name;

    private String description;

    private Integer docCount;

    private LocalDateTime createdTime;
}
