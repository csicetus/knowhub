package com.knowhub.controller;

import com.knowhub.common.result.Result;
import com.knowhub.dto.CreateKbRequest;
import com.knowhub.service.KnowledgeBaseService;
import com.knowhub.util.SecurityUtil;
import com.knowhub.vo.DocumentVO;
import com.knowhub.vo.KnowledgeBaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.List;

@Tag(name = "知识库管理", description = "知识库的增删改查")
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public Result<KnowledgeBaseVO> createKb(@Valid @RequestBody CreateKbRequest request) {
        return Result.success(knowledgeBaseService.createKb(request, SecurityUtil.getCurrentUserId()));
    }

    @Operation(summary = "上传文档")
    @PostMapping(value = "/{kbId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DocumentVO> uploadDocument(@PathVariable Long kbId,
                                             @RequestParam("file") @Parameter(description = "上传的文件") MultipartFile file) {
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
