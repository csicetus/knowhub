package com.knowhub.es.repository;

import com.knowhub.es.document.EsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsDocumentRepository extends ElasticsearchRepository<EsDocument, String> {
}
