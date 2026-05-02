package com.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** AI 智能分析模块配置属性 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai.intelligence")
public class AppAiIntelligenceProperties {

    private boolean enabled = true;

    private String chatSystemPrompt = "你是AquaSentinel水上安全智能助手。";

    private String alertAnalysisPromptTemplate;

    private EmbeddingConfig embedding = new EmbeddingConfig();

    private ChatHistoryConfig chatHistory = new ChatHistoryConfig();

    @Data
    public static class EmbeddingConfig {
        private boolean enabled = true;
        private double similarityThreshold = 0.7;
        private int maxSimilarResults = 5;
        private int recentDaysLimit = 30;
    }

    @Data
    public static class ChatHistoryConfig {
        private int maxConversationsPerUser = 50;
        private int maxMessagesPerConversation = 100;
    }
}
