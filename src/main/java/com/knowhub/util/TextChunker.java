package com.knowhub.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TextChunker {

    @Value("${app.rag.chunk-size}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap}")
    private int chunkOverlap;

    @PostConstruct
    public void validate() {
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap 必须小于 chunkSize，请检查配置");
        }
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start = start + chunkSize - chunkOverlap) {
            int end = Math.min(start + chunkSize, text.length());
            String chunkText = text.substring(start, end);
            chunks.add(chunkText);
        }

        return chunks;
    }
}
