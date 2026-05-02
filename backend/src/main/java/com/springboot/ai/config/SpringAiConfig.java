package com.springboot.ai.config;

import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/** Spring AI核心配置类 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "app.ai.intelligence.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SpringAiConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Primary
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Value(
                            "${spring.ai.openai.embedding.base-url:${spring.ai.openai.base-url:https://api.openai.com}}")
                    String embeddingBaseUrl,
            @Value("${spring.ai.openai.embedding.api-key:${spring.ai.openai.api-key:}}")
                    String embeddingApiKey,
            @Value("${spring.ai.openai.embedding.options.model:text-embedding-v4}")
                    String embeddingModelName) {
        log.info("创建 Embedding 模型, base-url={}, model={}", embeddingBaseUrl, embeddingModelName);
        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .requestInterceptor(
                                (request, body, execution) -> {
                                    ClientHttpResponse response = execution.execute(request, body);
                                    MediaType contentType = response.getHeaders().getContentType();
                                    if (contentType != null
                                            && !contentType.includes(MediaType.APPLICATION_JSON)) {
                                        log.info(
                                                "修正 Embedding 响应 content-type: {} -> application/json",
                                                contentType);
                                        return new ContentTypeFixingResponse(response);
                                    }
                                    return response;
                                });
        OpenAiApi embeddingApi =
                new OpenAiApi(
                        embeddingBaseUrl, embeddingApiKey, restClientBuilder, WebClient.builder());
        OpenAiEmbeddingOptions options =
                OpenAiEmbeddingOptions.builder().withModel(embeddingModelName).build();
        return new OpenAiEmbeddingModel(embeddingApi, MetadataMode.EMBED, options);
    }

    /** 将供应商返回的 octet-stream 修正为 JSON，避免 Spring AI 无法解析。 */
    private static class ContentTypeFixingResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;

        ContentTypeFixingResponse(ClientHttpResponse delegate) {
            this.delegate = delegate;
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            delegate.getHeaders().forEach(headers::addAll);
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return headers;
        }

        @Override
        public InputStream getBody() throws IOException {
            return delegate.getBody();
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
