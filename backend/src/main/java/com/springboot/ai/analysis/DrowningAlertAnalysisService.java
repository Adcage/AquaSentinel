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
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 溺水报警分析服务 */
@Slf4j
@Service("drowningAlertAnalysisService")
@ConditionalOnProperty(
        name = "app.ai.intelligence.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DrowningAlertAnalysisService implements AlertAnalysisService {

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
        return "DROWING";
    }

    private String buildPrompt(AlertRecord alert, List<AlertEmbedding> similarAlerts, Venue venue) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(aiProperties.getAlertAnalysisPromptTemplate())) {
            String template = aiProperties.getAlertAnalysisPromptTemplate();
            template =
                    template.replace(
                            "{alertType}",
                            alert.getAlert_type() != null ? alert.getAlert_type() : "溺水");
            template =
                    template.replace(
                            "{incidentLocation}",
                            alert.getIncident_location() != null
                                    ? alert.getIncident_location()
                                    : "未知");
            template =
                    template.replace(
                            "{detectionResult}",
                            alert.getDetection_result() != null
                                    ? alert.getDetection_result()
                                    : "未提供");
            template = template.replace("{confidence}", "高");
            template = template.replace("{durationSec}", "未知");
            template = template.replace("{ruleHits}", "未提供");
            template =
                    template.replace("{venueName}", venue != null ? venue.getVenue_name() : "未知场馆");
            template =
                    template.replace("{similarAlertCount}", String.valueOf(similarAlerts.size()));
            sb.append(template);
        } else {
            sb.append("请根据以下报警信息生成一段智能分析描述：\n\n");
            sb.append("报警类型：")
                    .append(alert.getAlert_type() != null ? alert.getAlert_type() : "溺水")
                    .append("\n");
            sb.append("报警位置：")
                    .append(
                            alert.getIncident_location() != null
                                    ? alert.getIncident_location()
                                    : "未知")
                    .append("\n");
            sb.append("检测结果：")
                    .append(
                            alert.getDetection_result() != null
                                    ? alert.getDetection_result()
                                    : "未提供")
                    .append("\n");
            sb.append("场馆信息：").append(venue != null ? venue.getVenue_name() : "未知场馆").append("\n");
            sb.append("历史相似报警数量：").append(similarAlerts.size()).append("\n\n");
            sb.append("请生成一段2-3句话的分析描述，包含事件严重程度判断、与历史报警的关联分析、建议采取的措施。");
        }

        return sb.toString();
    }
}
