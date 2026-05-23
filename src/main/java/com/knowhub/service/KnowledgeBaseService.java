package com.knowhub.service;

import com.knowhub.dto.CreateKbRequest;
import com.knowhub.vo.DocumentVO;
import com.knowhub.vo.KnowledgeBaseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseService {
    KnowledgeBaseVO createKb(CreateKbRequest request, Long userId);
    List<KnowledgeBaseVO> listKnowledgeBases(Long userId);
    DocumentVO uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file);
    List<DocumentVO> listDocuments(Long knowledgeBaseId, Long userId);
}
