package com.knowhub.service;

import com.knowhub.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    DocumentVO uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file);
    void generateAndStoreEmbeddings(List<String> chunks, Long documentId, Long knowledgeBaseId, Long userId);
    void vectorizeContent(String content, Long documentId, Long knowledgeBaseId, Long userId);
}
