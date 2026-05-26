package com.knowhub.mcp;

import com.knowhub.service.ChatService;
import com.knowhub.service.KnowledgeBaseService;
import com.knowhub.vo.DocumentVO;
import com.knowhub.vo.KnowledgeBaseVO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowHubMcpTools {

    private static final String LIST_KNOWLEDGEBASES = "listKnowledgeBases";
    private static final String LIST_DOCUMENTS = "listDocuments";
    private static final String ASK_QUESTIONS = "askQuestion";

    private final ChatService chatService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MeterRegistry meterRegistry;

    @Tool(description = "查询指定用户的所有知识库列表，需要传入用户ID")
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        meterRegistry.counter("knowhub.mcp.tool.calls", "tool", LIST_KNOWLEDGEBASES).increment();

        try {
            setAuthentication(userId);
            return knowledgeBaseService.listKnowledgeBases(userId);
        } catch (Exception e) {
            log.error("MCP {} 失败: userId={}", LIST_KNOWLEDGEBASES, userId, e);
            throw new RuntimeException("查询知识库列表失败: " + e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Tool(description = "查询指定知识库下的所有文档，需要传入知识库ID和用户ID")
    public List<DocumentVO> listDocuments(Long knowledgeBaseId, Long userId) {
        meterRegistry.counter("knowhub.mcp.tool.calls", "tool", LIST_DOCUMENTS).increment();

        try {
            setAuthentication(userId);
            return knowledgeBaseService.listDocuments(knowledgeBaseId, userId);
        } catch (Exception e) {
            log.error("MCP {} 失败: userId={}", LIST_DOCUMENTS, userId, e);
            throw new RuntimeException("查询知识库下的文档列表失败: " + e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Tool(description = "根据用户问题在指定知识库中进行智能问答，返回基于文档内容的答案")
    public String askQuestion(Long knowledgeBaseId, Long userId, String question) {
        meterRegistry.counter("knowhub.mcp.tool.calls", "tool", ASK_QUESTIONS).increment();

        try {
            setAuthentication(userId);
            return chatService.askQuestion(knowledgeBaseId, userId, question);
        } catch (Exception e) {
            log.error("MCP {} 失败: userId={}", ASK_QUESTIONS, userId, e);
            throw new RuntimeException("根据用户问题在指定知识库中进行智能问答失败: " + e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
