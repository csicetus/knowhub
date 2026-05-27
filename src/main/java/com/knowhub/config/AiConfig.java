package com.knowhub.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public DashScopeApi dashScopeApi(DashScopeConnectionProperties commonProperties,
                                     RestClient.Builder restClientBuilder,
                                     WebClient.Builder webClientBuilder,
                                     ResponseErrorHandler responseErrorHandler) {
        return DashScopeApi.builder()
                .apiKey(commonProperties.getApiKey())
                .baseUrl(commonProperties.getBaseUrl())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .responseErrorHandler(responseErrorHandler)
                .build();
    }
}