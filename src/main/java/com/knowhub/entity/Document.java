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
@TableName("document")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private Long userId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String filePath;

    // 0待处理 1向量化中 2完成 3失败
    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
