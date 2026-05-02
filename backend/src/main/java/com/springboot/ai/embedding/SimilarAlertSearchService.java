package com.springboot.ai.embedding;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.springboot.config.AppAiIntelligenceProperties;
import com.springboot.model.entity.AlertEmbedding;
import com.springboot.model.entity.AlertRecord;
import com.springboot.service.AlertEmbeddingService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 相似报警搜索服务 */
@Slf4j
@Service
@ConditionalOnProperty(
        name = "app.ai.intelligence.embedding.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SimilarAlertSearchService {

    @Resource private OpenAiEmbeddingModel embeddingModel;

    @Resource private AlertVectorRepository vectorRepository;

    @Resource private AlertEmbeddingService alertEmbeddingService;

    @Resource private AppAiIntelligenceProperties aiProperties;

    /** 搜索与指定报警相似的历史报警 */
    public List<AlertEmbedding> searchSimilar(Long alertId, int maxResults) {
        float[] queryEmbedding = vectorRepository.getEmbedding(alertId);
        if (queryEmbedding == null) {
            log.info("报警向量不存在，无法搜索: alertId={}", alertId);
            return Collections.emptyList();
        }

        int effectiveMaxResults =
                Math.min(maxResults, aiProperties.getEmbedding().getMaxSimilarResults());
        List<AlertVectorRepository.SimilarAlert> similar =
                vectorRepository.searchSimilar(
                        queryEmbedding,
                        effectiveMaxResults + 1,
                        aiProperties.getEmbedding().getSimilarityThreshold());

        return similar.stream()
                .filter(s -> !s.alertId().equals(alertId))
                .limit(effectiveMaxResults)
                .map(s -> alertEmbeddingService.getByAlertId(s.alertId()))
                .filter(Objects::nonNull)
                .toList();
    }

    /** 基于自然语言文本搜索相似报警 */
    public List<AlertEmbedding> searchByText(String queryText, int maxResults) {
        if (StringUtils.isBlank(queryText)) {
            return Collections.emptyList();
        }

        try {
            org.springframework.ai.document.Document document =
                    new org.springframework.ai.document.Document(queryText);
            float[] queryEmbedding = embeddingModel.embed(document);

            if (queryEmbedding == null || queryEmbedding.length == 0) {
                log.warn("查询文本嵌入为空: query={}", queryText);
                return Collections.emptyList();
            }

            log.info(
                    "查询文本嵌入成功: query={}, 维度={}, threshold={}",
                    queryText,
                    queryEmbedding.length,
                    aiProperties.getEmbedding().getSimilarityThreshold());

            List<AlertVectorRepository.SimilarAlert> similar =
                    vectorRepository.searchSimilar(
                            queryEmbedding,
                            maxResults,
                            aiProperties.getEmbedding().getSimilarityThreshold());

            log.info("相似报警搜索结果: query={}, 结果数={}", queryText, similar.size());

            return similar.stream()
                    .map(s -> alertEmbeddingService.getByAlertId(s.alertId()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("文本嵌入生成失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 为报警生成向量嵌入并存储 */
    public void generateAndStoreEmbedding(AlertRecord alert) {
        if (alert == null || alert.getId() == null) {
            return;
        }

        String sourceText = buildSourceText(alert);
        try {
            org.springframework.ai.document.Document document =
                    new org.springframework.ai.document.Document(sourceText);
            float[] embedding = embeddingModel.embed(document);

            if (embedding == null || embedding.length == 0) {
                log.warn("嵌入生成失败: alertId={}", alert.getId());
                return;
            }

            vectorRepository.store(
                    alert.getId(),
                    alert.getAlert_uid(),
                    sourceText,
                    embedding,
                    "text-embedding-v4");
            log.info("报警向量嵌入已生成并存储: alertId={}", alert.getId());
        } catch (Exception e) {
            log.error("报警向量嵌入生成失败: alertId={}, error={}", alert.getId(), e.getMessage());
        }
    }

    private String buildSourceText(AlertRecord alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("报警类型：")
                .append(alert.getAlert_type() != null ? alert.getAlert_type() : "未知")
                .append(" ");
        sb.append("位置：")
                .append(alert.getIncident_location() != null ? alert.getIncident_location() : "未知")
                .append(" ");
        sb.append("检测结果：")
                .append(alert.getDetection_result() != null ? alert.getDetection_result() : "未提供");
        if (alert.getAi_analysis() != null) {
            sb.append(" AI分析：").append(alert.getAi_analysis());
        }
        return sb.toString();
    }
}
