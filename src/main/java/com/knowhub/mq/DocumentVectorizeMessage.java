package com.knowhub.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVectorizeMessage {

    private Long documentId;

    private Long knowledgeBaseId;

    private Long userId;

    private String fileName;

    private String filePath;
}
