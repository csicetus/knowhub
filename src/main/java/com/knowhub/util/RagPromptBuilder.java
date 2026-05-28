package com.knowhub.util;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.knowhub.es.document.EsDocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RagPromptBuilder {

    private final DashScopeApi dashScopeApi;
    private final ElasticsearchOperations elasticsearchOperations;
    private final VectorStore vectorStore;

    @Value("${app.dashscope.rerank-model}")
    private String rerankModel;

    @Value("${app.rag.top-k}")
    private int topK;

    public RagPromptBuilder(
            ElasticsearchOperations elasticsearchOperations,
            DashScopeApi dashScopeApi,
            VectorStore vectorStore) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.dashScopeApi = dashScopeApi;
        this.vectorStore = vectorStore;
    }

    /*
     * ===== 两阶段检索 (Retrieve)
     * 阶段一：召回 (Recall) - 找到尽量多的相关文档
     * 向量检索：把问题转成 Embedding，再 pgvector 里找语义类似的文档块
     * 全文检索：用 ES match query 找包含关键词的文档块，支持中英文分词
     * 两边各取 topK * 4，保证召回率
     *
     * 阶段二：精排 (Rank) - 从召回结果里挑出最相关的
     * RRF 融合：合并两路结果，同时出现在两路的文档排名更高
     * Rerank: 用专门的相关模型（gte-rerank）对候选文档重新打分排序
     *         比向量相似度更准确，最终取 topK 个
     *
     * ===== 生成（Generate） =====
     * 把 topK 个文档块拼成 context，加上对话历史，构建 Prompt 给 LLM
     * LLM 基于 context 生成答案，减少幻觉
     */
    public String buildPrompt(Long knowledgeBaseId,
                              Long userId,
                              String question,
                              StringBuilder recentChatHistory) {
        // 1. 向量检索：找和用户问题最相关的 top-k * 4 个文档块
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var filter = b.and(
                b.eq("knowledgeBaseId", knowledgeBaseId),
                b.eq("userId", userId)
        );

        List<Document> vectorDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK * 4)
                        .filterExpression(filter.build())
                        .build()
        );
        if (vectorDocs == null) {
            vectorDocs = List.of();
        }

        // 2. 全文检索 top-k * 4
        List<Document> fullTextDocs =
                fullTextSearch(knowledgeBaseId, userId, question, topK * 4);

        // 3. RRF 融合，取 top-k * 4
        List<Document> relatedDocs = rrfFusion(vectorDocs, fullTextDocs, topK * 4);

        // 4. Rerank，最终取 top-k
        List<Document> reranked = rerank(question, relatedDocs, topK);
        log.debug("向量检索结果: {} 条, 全文检索结果: {} 条, RRF融合后: {} 条, Rerank后: {} 条",
                vectorDocs.size(), fullTextDocs.size(), relatedDocs.size(), reranked.size());

        // 5. 把检索到的文档块拼成上下文字符串
        String context = reranked.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 6. 构建 Prompt
        return String.format("""
                你是一个企业知识库助手，请根据以下参考内容回答用户问题。
                如果参考内容中没有相关信息，请如实告知。
                
                参考内容：
                %s
                
                对话历史：
                %s
                
                用户问题：%s
                """, context, recentChatHistory, question);
    }

    /**
     * Reciprocal Rank Fusion 算法融合两路检索结果
     * 论文来源：<a href="https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf">RRF Paper</a>
     * score(d) = Σ 1/(k + rank(d, r))，k=60 是经验常数
     */
    private List<Document> rrfFusion(List<Document> vectorDocs,
                                     List<Document> fullTextDocs,
                                     int limit) {
        final int K = 60;
        Map<String, Double> scores = new HashMap<>();

        // 向量检索结果按排名计算分数
        for (int i = 0; i < vectorDocs.size(); i++) {
            String content = vectorDocs.get(i).getText();
            scores.merge(content, 1.0 / (K + i), Double::sum);
        }

        // 全文检索结果按排名计算分数
        for (int i = 0; i < fullTextDocs.size(); i++) {
            String content = fullTextDocs.get(i).getText();
            scores.merge(content, 1.0 / (K + i), Double::sum);
        }

        // 按分数降序，取 top-k
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new Document(entry.getKey()))
                .collect(Collectors.toList());
    }

    private List<Document> rerank(String question, List<Document> candidates, int topN) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        DashScopeApi.RerankRequestInput rerankRequestInput =
                new DashScopeApi.RerankRequestInput(question,
                        candidates.stream()
                                .map(Document::getText)
                                .collect(Collectors.toList()));
        DashScopeApi.RerankRequest request =
                new DashScopeApi.RerankRequest(rerankModel,
                        rerankRequestInput,
                        new DashScopeApi.RerankRequestParameter(topN, false));

        try {
            ResponseEntity<DashScopeApi.RerankResponse> response = dashScopeApi.rerankEntity(request);
            DashScopeApi.RerankResponse body = response.getBody();
            if (body == null || body.output() == null) {
                return candidates.subList(0, Math.min(topN, candidates.size()));
            }

            return body.output().results().stream()
                    .map(r -> candidates.get(r.index()))
                    .limit(topN)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Rerank API 调用失败，降级为原始顺序: {}", e.getMessage());
            return candidates.subList(0, Math.min(topN, candidates.size()));
        }
    }

    private List<Document> fullTextSearch(Long knowledgeBaseId,
                                          Long userId,
                                          String question,
                                          int limit) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                // 全文检索：content 包含用户问题
                                .must(m -> m
                                        .match(mt -> mt
                                                .field("content")
                                                .query(question)))
                                // 精确过滤：只搜当前知识库
                                .filter(f -> f.term(t -> t
                                        .field("knowledgeBaseId")
                                        .value(knowledgeBaseId)))
                                // 精确过滤：只搜当前用户
                                .filter(f -> f.term(t -> t
                                        .field("userId")
                                        .value(userId)))
                        )
                )
                .withMaxResults(limit)
                .build();

        SearchHits<EsDocumentChunk> hits = elasticsearchOperations.search(query, EsDocumentChunk.class);

        return hits.stream()
                .map(hit -> new Document(hit.getContent().getContent()))
                .collect(Collectors.toList());
    }
}
