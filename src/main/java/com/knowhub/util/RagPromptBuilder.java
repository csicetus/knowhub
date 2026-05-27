package com.knowhub.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RagPromptBuilder {

    private final JdbcTemplate vectorJdbcTemplate;
    private final VectorStore vectorStore;

    @Value("${app.rag.top-k}")
    private int topK;

    public RagPromptBuilder(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            VectorStore vectorStore) {
        this.vectorJdbcTemplate = vectorJdbcTemplate;
        this.vectorStore = vectorStore;
    }

    public String buildPrompt(Long knowledgeBaseId,
                              Long userId,
                              String question,
                              StringBuilder recentChatHistory) {
        // 1. 向量检索：找和用户问题最相关的 top-k*2 个文档块
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var filter = b.and(
                b.eq("knowledgeBaseId", knowledgeBaseId),
                b.eq("userId", userId)
        );

        List<Document> vectorDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK * 2)
                        .filterExpression(filter.build())
                        .build()
        );
        if (vectorDocs == null) {
            vectorDocs = List.of();
        }

        // 2. 全文检索 top-k*2
        List<Document> fullTextDocs =
                fullTextSearch(knowledgeBaseId, userId, question, topK * 2);

        // 3. RRF 融合，最终取 top-k
        List<Document> relatedDocs = rrfFusion(vectorDocs, fullTextDocs, topK);
        log.debug("向量检索结果: {} 条, 全文检索结果: {} 条, RRF融合后: {} 条",
                vectorDocs.size(), fullTextDocs.size(), relatedDocs.size());

        // 4. 把检索到的文档块拼成上下文字符串
        String context  = relatedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 5. 构建 Prompt
        return String.format("""
                    你是一个企业知识库助手，请根据以下参考内容回答用户问题。
                    如果参考内容中没有相关信息，请如实告知。
                    
                    参考内容：
                    %s
                    
                    对话历史：
                    %s
                    
                    用户问题：%s
                    """, context , recentChatHistory, question);
    }

    private List<Document> fullTextSearch(Long knowledgeBaseId,
                                          Long userId,
                                          String question,
                                          int limit) {
        String sqlQuery = """
            SELECT content, metadata,
                   ts_rank(to_tsvector('english', content),
                           plainto_tsquery('english', ?)) as rank
            FROM vector_store
            WHERE to_tsvector('english', content) @@ plainto_tsquery('english', ?)
              AND metadata->>'userId' = ?
              AND metadata->>'knowledgeBaseId' = ?
            ORDER BY rank DESC
            LIMIT ?
            """;

        List<Map<String, Object>> rows = vectorJdbcTemplate.queryForList(sqlQuery,
                question, question, userId.toString(), knowledgeBaseId.toString(), limit);
        return rows.stream()
                .map(row -> new Document((String) row.get("content")))
                .collect((Collectors.toList()));
    }

    /**
     * Reciprocal Rank Fusion 算法融合两路检索结果
     * 论文来源：<a href="https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf">RRF Paper</a>     * score(d) = Σ 1/(k + rank(d, r))，k=60 是经验常数
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
}
