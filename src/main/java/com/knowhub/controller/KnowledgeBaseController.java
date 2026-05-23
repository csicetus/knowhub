package com.knowhub.controller;

import com.knowhub.common.result.Result;
import com.knowhub.dto.CreateKbRequest;
import com.knowhub.service.KnowledgeBaseService;
import com.knowhub.util.SecurityUtil;
import com.knowhub.vo.DocumentVO;
import com.knowhub.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public Result<KnowledgeBaseVO> createKb(@Valid @RequestBody CreateKbRequest request) {
        return Result.success(knowledgeBaseService.createKb(request, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/{kbId}/documents")
    public Result<DocumentVO> uploadDocument(@PathVariable Long kbId,
                                             @RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeBaseService.uploadDocument(kbId, SecurityUtil.getCurrentUserId(), file));
    }

    @GetMapping
    public Result<List<KnowledgeBaseVO>> listKnowledgeBases() {
        return Result.success(knowledgeBaseService.listKnowledgeBases(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{kbId}/documents")
    public Result<List<DocumentVO>> listDocuments(@PathVariable Long kbId) {
        return Result.success(knowledgeBaseService.listDocuments(kbId, SecurityUtil.getCurrentUserId()));
    }
}
