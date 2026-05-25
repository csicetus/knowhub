package com.knowhub.util;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final VectorStore vectorStore;

    @Value("${app.rag.top-k}")
    private int topK;

    public String buildPrompt(Long knowledgeBaseId,
                               Long userId,
                               String question,
                               StringBuilder recentChatHistory) {
        // 1. 向量检索：找和用户问题最相关的 top-k 个文档块
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var filter = b.and(
                b.eq("knowledgeBaseId", knowledgeBaseId),
                b.eq("userId", userId)
        );

        List<Document> relatedDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .filterExpression(filter.build())
                        .build()
        );

        // 2. 把检索到的文档块拼成上下文字符串
        String context  = relatedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 3. 构建 Prompt
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

}
