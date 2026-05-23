package com.knowhub.service;

import com.knowhub.common.exception.BusinessException;
import com.knowhub.entity.KnowledgeBase;
import com.knowhub.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseValidator {
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBase validateAndGet(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || !knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(404, "知识库不存在或无权访问");
        }
        return knowledgeBase;
    }
}
