package com.knowhub.canal;

import com.knowhub.entity.Document;
import com.knowhub.es.document.EsDocument;
import com.knowhub.es.repository.EsDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

@Slf4j
@CanalTable("document")
@Component
@RequiredArgsConstructor
public class DocumentCanalListener implements EntryHandler<Document> {

    private final EsDocumentRepository esDocumentRepository;

    @Override
    public void insert(Document document) {
        buildEsDocument(document);
        log.info("插入 ES 文档 {}", document.getId());
    }

    @Override
    public void update(Document before, Document after) {
        buildEsDocument(after);
        log.info("更新 ES 文档，before {}，after {}", before.getId(), after.getId());
    }

    private void buildEsDocument(Document doc) {
        EsDocument esDocument = EsDocument.builder()
                .id(String.valueOf(doc.getId()))
                .knowledgeBaseId(doc.getKnowledgeBaseId())
                .userId(doc.getUserId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .build();

        esDocumentRepository.save(esDocument);
    }

    @Override
    public void delete(Document document) {
        esDocumentRepository.deleteById(String.valueOf(document.getId()));
        log.info("删除 ES 文档 {}", document.getId());
    }
}
