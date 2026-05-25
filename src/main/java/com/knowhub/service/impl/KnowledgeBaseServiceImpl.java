package com.knowhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowhub.dto.CreateKbRequest;
import com.knowhub.entity.Document;
import com.knowhub.entity.KnowledgeBase;
import com.knowhub.mapper.DocumentMapper;
import com.knowhub.mapper.KnowledgeBaseMapper;
import com.knowhub.service.DocumentService;
import com.knowhub.service.KnowledgeBaseService;
import com.knowhub.service.KnowledgeBaseValidator;
import com.knowhub.vo.DocumentVO;
import com.knowhub.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String KB_LIST_KEY = "kb:list:userId:";
    private static final String DOC_LIST_KEY = "doc:list:kbId:";

    private final DocumentMapper documentMapper;
    private final DocumentService documentService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseValidator knowledgeBaseValidator;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public KnowledgeBaseVO createKb(CreateKbRequest request, Long userId) {
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .docCount(0)
                .build();

        knowledgeBaseMapper.insert(knowledgeBase);

        KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
        knowledgeBaseVO.setId(knowledgeBase.getId());
        knowledgeBaseVO.setUserId(userId);
        knowledgeBaseVO.setName(knowledgeBase.getName());
        knowledgeBaseVO.setDescription(knowledgeBase.getDescription());
        knowledgeBaseVO.setDocCount(0);
        knowledgeBaseVO.setCreatedTime(knowledgeBase.getCreatedTime());

        redisTemplate.delete(KB_LIST_KEY + userId);

        return knowledgeBaseVO;
    }

    // Cache Aside 旁路缓存模式
    // 查询先走 Redis，缓存未命中再查 MySQL，结果写回 Redis
    // 数据变更时 createKb 主动删除缓存，保证一致性
    @Override
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        String cacheKey = KB_LIST_KEY + userId;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            try {
                return objectMapper.readValue((String) cache, new TypeReference<List<KnowledgeBaseVO>>() {});
            } catch (JsonProcessingException e) {
                log.error("Redis 反序列化失败", e);
                return Collections.emptyList();
            }
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(
          new LambdaQueryWrapper<KnowledgeBase>()
                  .orderByDesc(KnowledgeBase::getCreatedTime)
        );

        if (knowledgeBases.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, "[]", 1, TimeUnit.MINUTES);
            return Collections.emptyList();
        }

        List<KnowledgeBaseVO> voList = toKnowledgeBaseVO(knowledgeBases);
        redisTemplate.opsForValue().set(cacheKey, voList, 5, TimeUnit.MINUTES);
        return voList;
    }

    private List<KnowledgeBaseVO> toKnowledgeBaseVO(List<KnowledgeBase> knowledgeBases) {
        return knowledgeBases.stream()
                .map(kb -> {
                    KnowledgeBaseVO vo = new KnowledgeBaseVO();
                    vo.setId(kb.getId());
                    vo.setUserId(kb.getUserId());
                    vo.setName(kb.getName());
                    vo.setDescription(kb.getDescription());
                    vo.setDocCount(kb.getDocCount());
                    vo.setCreatedTime(kb.getCreatedTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public DocumentVO uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file) {
        return documentService.uploadDocument(knowledgeBaseId, userId, file);
    }

    // Cache Aside 旁路缓存模式
    // 查询先走 Redis，缓存未命中再查 MySQL，结果写回 Redis
    // 数据变更时 uploadDocument 主动删除缓存，保证一致性
    @Override
    public List<DocumentVO> listDocuments(Long knowledgeBaseId, Long userId) {
        knowledgeBaseValidator.validateAndGet(knowledgeBaseId, userId);

        String cacheKey = DOC_LIST_KEY + knowledgeBaseId;
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            try {
                return objectMapper.readValue((String) cache, new TypeReference<List<DocumentVO>>() {});
            } catch (JsonProcessingException e) {
                log.error("Redis 反序列化失败", e);
                return Collections.emptyList();
            }
        }

        List<Document> documents = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId));

        if (documents.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, "[]]", 1, TimeUnit.MINUTES);
            return Collections.emptyList();
        }

        List<DocumentVO> voList = toDocumentVO(documents);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(voList), 3, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Redis 序列化失败", e);
        }

        return voList;
    }

    private List<DocumentVO> toDocumentVO(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    DocumentVO vo = new DocumentVO();
                    vo.setId(doc.getId());
                    vo.setKnowledgeBaseId(doc.getKnowledgeBaseId());
                    vo.setFileName(doc.getFileName());
                    vo.setFileSize(doc.getFileSize());
                    vo.setFileType(doc.getFileType());
                    vo.setStatus(doc.getStatus());
                    vo.setChunkCount(doc.getChunkCount());
                    vo.setErrorMsg(doc.getErrorMsg());
                    vo.setCreatedTime(doc.getCreatedTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
