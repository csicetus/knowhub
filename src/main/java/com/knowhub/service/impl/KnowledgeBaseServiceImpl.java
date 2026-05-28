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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

        log.info("删除知识库列表缓存:" + KB_LIST_KEY + userId);
        redisTemplate.delete(KB_LIST_KEY + userId);
        log.info("缓存删除完成");

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

        List<DocumentVO> voList = buildDocumentVOList(documents);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(voList), 3, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Redis 序列化失败", e);
        }

        return voList;
    }

    private List<DocumentVO> buildDocumentVOList(List<Document> documents) {
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

    // ========== @Transactional 失效场景演示（面试素材，非业务代码）==========

    /**
     * 失效场景一：同类内部调用（最常见）
     * this.innerMethod() 绕过了 AOP 代理，innerMethod 的事务不生效
     */
    @Transactional
    public void outerMethod() {
        knowledgeBaseMapper.insert(new KnowledgeBase()); // 有事务
        this.innerMethod(); // 直接调用，绕过代理，innerMethod 事务不生效
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void innerMethod() {
        // 期望开启新事务，实际上没有，因为被 this 调用
        knowledgeBaseMapper.insert(new KnowledgeBase());
    }

    /**
     * 失效场景二：异常被吞掉，事务不回滚
     */
    @Transactional
    public void swallowException() {
        try {
            knowledgeBaseMapper.insert(new KnowledgeBase());
            throw new RuntimeException("出错了");
        } catch (Exception e) {
            log.error("异常被吞掉", e);
            // 没有 rethrow，Spring 不知道发生了异常，事务正常提交，不回滚
        }
    }

    /**
     * 失效场景三：异常类型不对，默认只回滚 RuntimeException
     */
    @Transactional // 默认 rollbackFor = RuntimeException.class
    public void wrongExceptionType() throws Exception {
        knowledgeBaseMapper.insert(new KnowledgeBase());
        throw new Exception("checked exception，不会回滚");
        // 正确写法：@Transactional(rollbackFor = Exception.class)
    }

    /**
     * 失效场景四：多线程，事务上下文不跨线程传递
     * 和 SecurityContext 问题同理，ThreadLocal 线程隔离
     */
    @Transactional
    public void multiThread() {
        knowledgeBaseMapper.insert(new KnowledgeBase()); // 主线程，有事务

        new Thread(() -> {
            // 新线程，ThreadLocal 是空的，没有事务上下文
            // 这里的数据库操作不在主线程的事务里
            knowledgeBaseMapper.insert(new KnowledgeBase());
        }).start();
    }

    /**
     * 失效场景五：方法不是 public
     * Spring AOP 只拦截 public 方法，private 方法上的 @Transactional 不生效
     * IDEA 会直接报错：Methods annotated with '@Transactional' must be overridable
     *
     * 错误示例：
     * @Transactional
     * private void privateMethod() {  // private 方法，事务不生效
     *     knowledgeBaseMapper.insert(new KnowledgeBase());
     * }
     *
     * 正确写法：改成 public
     * @Transactional
     * public void publicMethod() { ... }
     */

    /**
     * 失效场景六：Bean 未被 Spring 管理
     * 没有 @Service/@Component 等注解，不是 Spring Bean
     * AOP 代理不会生效，@Transactional 无效
     * （本类有 @Service，此处仅注释说明）
     *
     * 错误示例：
     * public class SomeClass {  // 没有 @Service
     *     @Transactional
     *     public void doSomething() { ... }  // 事务不生效
     * }
     */

    /**
     * 失效场景七：数据库引擎不支持事务
     * MySQL 的 MyISAM 引擎不支持事务，只有 InnoDB 支持
     * 我们建表时已指定 ENGINE=InnoDB，所以没有这个问题
     *
     * 错误示例（建表时）：
     * CREATE TABLE test (...) ENGINE=MyISAM;  -- 不支持事务
     * CREATE TABLE test (...) ENGINE=InnoDB;  -- 支持事务（正确）
     */
}
