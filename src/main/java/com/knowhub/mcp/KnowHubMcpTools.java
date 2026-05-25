package com.knowhub.mcp;

import com.knowhub.service.ChatService;
import com.knowhub.service.KnowledgeBaseService;
import com.knowhub.vo.DocumentVO;
import com.knowhub.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KnowHubMcpTools {

    private final ChatService chatService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Tool(description = "查询指定用户的所有知识库列表，需要传入用户ID")
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        setAuthentication(userId);
        try {
            return knowledgeBaseService.listKnowledgeBases(userId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Tool(description = "查询指定知识库下的所有文档，需要传入知识库ID和用户ID")
    public List<DocumentVO> listDocuments(Long knowledgeBaseId, Long userId) {
        setAuthentication(userId);
        try {
            return knowledgeBaseService.listDocuments(knowledgeBaseId, userId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Tool(description = "根据用户问题在指定知识库中进行智能问答，返回基于文档内容的答案")
    public String askQuestion(Long knowledgeBaseId, Long userId, String question) {
        setAuthentication(userId);
        try {
            return chatService.askQuestion(knowledgeBaseId, userId, question);
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
