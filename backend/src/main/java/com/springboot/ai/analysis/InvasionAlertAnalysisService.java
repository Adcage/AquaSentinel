package com.springboot.ai.analysis;

import java.util.List;

import com.springboot.ai.chat.ChatService;
import com.springboot.ai.embedding.SimilarAlertSearchService;
import com.springboot.config.AppAiIntelligenceProperties;
import com.springboot.model.entity.AlertEmbedding;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.Venue;
import com.springboot.service.AlertRecordService;
import com.springboot.service.VenueService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 入侵报警分析服务（区域入侵类型） */
@Slf4j
@Service("invasionAlertAnalysisService")
@ConditionalOnProperty(
        name = "app.ai.intelligence.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvasionAlertAnalysisService implements AlertAnalysisService {

    @Resource private ChatService chatService;

    @Resource private AppAiIntelligenceProperties aiProperties;

    @Resource private AlertRecordService alertRecordService;

    @Resource private VenueService venueService;

    @Resource private SimilarAlertSearchService similarAlertSearchService;

    @Override
    public String analyzeAlert(Long alertId) {
        AlertRecord alert = alertRecordService.getById(alertId);
        if (alert == null) {
            log.warn("报警分析失败: alertId={}, 原因=报警不存在", alertId);
            return null;
        }

        try {
            List<AlertEmbedding> similarAlerts =
                    similarAlertSearchService.searchSimilar(
                            alertId, aiProperties.getEmbedding().getMaxSimilarResults());

            Venue venue = null;
            if (alert.getVenue_id() != null) {
                venue = venueService.getById(alert.getVenue_id());
            }

            String prompt = buildPrompt(alert, similarAlerts, venue);

            String analysis =
                    chatService.chatWithSystemPrompt(aiProperties.getChatSystemPrompt(), prompt);

            log.info(
                    "报警分析完成: alertId={}, 分析长度={}",
                    alertId,
                    analysis != null ? analysis.length() : 0);
            return analysis;
        } catch (Exception e) {
            log.error("AI报警分析失败: alertId={}, error={}", alertId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getSupportedAlertType() {
        return "CROSS_BORDER";
    }

    private String buildPrompt(AlertRecord alert, List<AlertEmbedding> similarAlerts, Venue venue) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下区域入侵报警信息生成一段智能分析描述：\n\n");
        sb.append("报警类型：区域入侵\n");
        sb.append("报警位置：")
                .append(alert.getIncident_location() != null ? alert.getIncident_location() : "未知")
                .append("\n");
        sb.append("检测结果：")
                .append(alert.getDetection_result() != null ? alert.getDetection_result() : "未提供")
                .append("\n");
        sb.append("场馆信息：").append(venue != null ? venue.getVenue_name() : "未知场馆").append("\n");
        sb.append("历史相似报警数量：").append(similarAlerts.size()).append("\n\n");
        sb.append("请分析：是否为误报可能性（如开放时间内的正常进入）、与历史入侵报警的对比、建议处理措施。");
        sb.append("生成一段2-3句话的简要分析。");

        return sb.toString();
    }
}
