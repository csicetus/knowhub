package com.knowhub.service.impl;

import com.knowhub.common.exception.BusinessException;
import com.knowhub.mapper.DocumentMapper;
import com.knowhub.service.DocumentService;
import com.knowhub.service.KnowledgeBaseValidator;
import com.knowhub.util.TextChunker;
import com.knowhub.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String DOC_LIST_KEY = "doc:list:kbId:";
    private static final String LOCK_KEY = "lock:doc:upload:";

    private final DocumentMapper documentMapper;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;
    private final KnowledgeBaseValidator knowledgeBaseValidator;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.upload.path}")
    private String uploadBasePath;

    @Override
    public DocumentVO uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file) {
        String lockKey = LOCK_KEY + knowledgeBaseId + ":" + file.getName();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            knowledgeBaseValidator.validateAndGet(knowledgeBaseId, userId);

            String fileName = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;
            Path uploadPath = Paths.get(uploadBasePath, String.valueOf(userId));
            Path filePath = uploadPath.resolve(uniqueFileName);
            try {
                Files.createDirectories(uploadPath);
                Files.write(filePath, file.getBytes());
            } catch (IOException e) {
                log.error("文件保存失败: {}", uniqueFileName, e);
                throw new BusinessException(500, "文件保存失败");        }

            com.knowhub.entity.Document document = com.knowhub.entity.Document.builder()
                    .knowledgeBaseId(knowledgeBaseId)
                    .userId(userId)
                    .fileName(uniqueFileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .filePath(String.valueOf(filePath))
                    .status(0)
                    .build();
            documentMapper.insert(document);

            int chunkSize;
            try {
                String content = parseDocument(file);
                List<String> chunks = textChunker.chunk(content);
                chunkSize = chunks.size();
                generateAndStoreEmbeddings(chunks, document.getId(), knowledgeBaseId, userId);
                document.setStatus(2);
                document.setChunkCount(chunkSize);
                documentMapper.updateById(document);
            } catch (Exception e) {
                log.error("文档处理失败: documentId={}", document.getId(), e);
                document.setStatus(3);
                document.setErrorMsg(e.getMessage());
                documentMapper.updateById(document);
                throw new BusinessException(500, "文档处理失败: " + e.getMessage());
            }

            DocumentVO documentVO = new DocumentVO();
            documentVO.setId((document.getId()));
            documentVO.setKnowledgeBaseId(knowledgeBaseId);
            documentVO.setFileName(uniqueFileName);
            documentVO.setFileType(document.getFileType());
            documentVO.setFileSize(document.getFileSize());
            documentVO.setStatus(document.getStatus());
            documentVO.setChunkCount(chunkSize);
            documentVO.setCreatedTime(document.getCreatedTime());

            redisTemplate.delete(DOC_LIST_KEY + knowledgeBaseId);

            return documentVO;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public String parseDocument(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".txt") && !fileName.endsWith(".md"))) {
            throw new BusinessException(400, "只支持 txt 和 md 格式");
        }

        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("文件读取失败: {}", fileName, e);
            throw new BusinessException(500, "文件读取失败");
        }
    }

    // RAG 核心：把文本块转成向量存入 pgvector
    // metadata 存 userId 和 knowledgeBaseId，用于多租户隔离
    // 检索时用 FilterExpression 过滤，防止用户 A 看到用户 B 的数据
    @Override
    public void generateAndStoreEmbeddings(List<String> chunks,
                                           Long documentId,
                                           Long knowledgeBaseId,
                                           Long userId) {
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("knowledgeBaseId", knowledgeBaseId);
            metadata.put("userId", userId);
            metadata.put("chunkIndex", i);

            Document document = Document.builder()
                    .text(chunk)
                    .metadata(metadata)
                    .build();
            documents.add(document);
        }

        vectorStore.add(documents);

        log.info("文档向量化完成: documentId={}, chunks={}", documentId, chunks.size());
    }
}
