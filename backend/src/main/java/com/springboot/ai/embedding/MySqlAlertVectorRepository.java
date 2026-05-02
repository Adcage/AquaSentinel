package com.springboot.ai.embedding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.springboot.config.AppAiIntelligenceProperties;
import com.springboot.model.entity.AlertEmbedding;
import com.springboot.service.AlertEmbeddingService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** MySQL向量仓库实现 使用JSON列存储向量，Java层计算余弦相似度 */
@Slf4j
@Repository
@ConditionalOnProperty(
        name = "app.ai.intelligence.embedding.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MySqlAlertVectorRepository implements AlertVectorRepository {

    @Resource private AlertEmbeddingService alertEmbeddingService;

    @Resource private AppAiIntelligenceProperties aiProperties;

    @Override
    public void store(
            Long alertId, String alertUid, String sourceText, float[] embedding, String model) {
        AlertEmbedding existing = alertEmbeddingService.getByAlertId(alertId);
        if (existing != null) {
            alertEmbeddingService.updateEmbedding(alertId, toJsonArray(embedding));
            return;
        }

        AlertEmbedding entity = new AlertEmbedding();
        entity.setAlert_id(alertId);
        entity.setAlert_uid(alertUid);
        entity.setSource_text(sourceText);
        entity.setEmbedding(toJsonArray(embedding));
        entity.setEmbedding_model(model);
        entity.setSimilarity_search_text(sourceText);
        entity.setCreated_at(new java.util.Date());
        entity.setUpdated_at(new java.util.Date());
        entity.setIs_delete(0);
        alertEmbeddingService.saveEmbedding(entity);
    }

    @Override
    public float[] getEmbedding(Long alertId) {
        AlertEmbedding embedding = alertEmbeddingService.getByAlertId(alertId);
        if (embedding == null || embedding.getEmbedding() == null) {
            return null;
        }
        return fromJsonArray(embedding.getEmbedding());
    }

    @Override
    public List<SimilarAlert> searchSimilar(float[] queryEmbedding, int topK, double threshold) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        List<AlertEmbedding> allEmbeddings =
                alertEmbeddingService.listRecentEmbeddings(
                        aiProperties.getEmbedding().getRecentDaysLimit());

        List<SimilarAlert> results = new ArrayList<>();
        for (AlertEmbedding emb : allEmbeddings) {
            if (emb.getEmbedding() == null) {
                continue;
            }
            float[] storedEmbedding = fromJsonArray(emb.getEmbedding());
            if (storedEmbedding == null || storedEmbedding.length != queryEmbedding.length) {
                continue;
            }
            double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);
            if (similarity >= threshold) {
                results.add(new SimilarAlert(emb.getAlert_id(), emb.getAlert_uid(), similarity));
            }
        }

        results.sort(Comparator.comparingDouble(SimilarAlert::similarity).reversed());
        return results.stream().limit(topK).toList();
    }

    @Override
    public void delete(Long alertId) {
        AlertEmbedding existing = alertEmbeddingService.getByAlertId(alertId);
        if (existing != null) {
            AlertEmbedding update = new AlertEmbedding();
            update.setId(existing.getId());
            update.setIs_delete(1);
            update.setUpdated_at(new java.util.Date());
            alertEmbeddingService.updateEmbedding(alertId, null);
        }
    }

    private String toJsonArray(float[] arr) {
        if (arr == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] fromJsonArray(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
            }
            if (json.isEmpty()) return new float[0];
            String[] parts = json.split(",");
            float[] arr = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                arr[i] = Float.parseFloat(parts[i].trim());
            }
            return arr;
        } catch (Exception e) {
            log.warn("解析向量JSON失败: {}", e.getMessage());
            return null;
        }
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
