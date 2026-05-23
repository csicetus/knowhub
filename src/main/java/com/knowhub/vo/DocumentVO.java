package com.knowhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {

    private Long id;

    private Long knowledgeBaseId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    private LocalDateTime createdTime;
}
