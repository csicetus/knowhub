package com.knowhub.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class VectorStoreConfig {

    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource(
            @Value("${spring.datasource-vector.url}") String url,
            @Value("${spring.datasource-vector.username}") String username,
            @Value("${spring.datasource-vector.password}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate((dataSource));
    }

    @Bean
    public PgVectorStore vectorStore(@Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
                                     EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.IVFFLAT)
                .initializeSchema(true)
                .build();
    }
}
