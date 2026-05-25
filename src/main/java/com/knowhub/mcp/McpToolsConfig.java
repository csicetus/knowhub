package com.knowhub.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider knowHubTools(KnowHubMcpTools knowHubMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(knowHubMcpTools)
                .build();
    }
}
