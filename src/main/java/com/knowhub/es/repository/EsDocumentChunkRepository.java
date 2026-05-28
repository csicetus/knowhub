package com.knowhub.es.repository;

import com.knowhub.es.document.EsDocumentChunk;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsDocumentChunkRepository extends ElasticsearchRepository<EsDocumentChunk, String> {
}
