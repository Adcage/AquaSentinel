package com.springboot.ai.analysis;

import java.util.Date;
import java.util.Map;

import com.springboot.ai.embedding.SimilarAlertSearchService;
import com.springboot.model.entity.AlertRecord;
import com.springboot.service.AlertRecordService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 报警分析事件监听器，异步处理报警分析请求。 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.ai.intelligence.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AlertAnalysisListener {

    @Resource private Map<String, AlertAnalysisService> analysisServices;

    @Resource private AlertRecordService alertRecordService;

    @Resource private SimilarAlertSearchService similarAlertSearchService;

    @Async
    @EventListener
    public void onAlertAnalysisRequest(AlertAnalysisEvent event) {
        Long alertId = event.getAlertId();
        String alertType = event.getAlertType();

        log.info("开始异步报警分析: alertId={}, alertType={}", alertId, alertType);

        AlertAnalysisService service = resolveService(alertType);
        if (service == null) {
            log.info("无匹配的分析服务: alertType={}, 跳过分析", alertType);
            return;
        }

        try {
            String analysis = service.analyzeAlert(alertId);
            if (analysis != null) {
                AlertRecord update = new AlertRecord();
                update.setId(alertId);
                update.setAi_analysis(analysis);
                update.setUpdated_at(new Date());
                alertRecordService.updateById(update);
                log.info("报警分析结果已保存: alertId={}", alertId);
            }
        } catch (Exception e) {
            log.error("报警分析执行失败: alertId={}, error={}", alertId, e.getMessage(), e);
        }

        try {
            AlertRecord alert = alertRecordService.getById(alertId);
            if (alert != null) {
                similarAlertSearchService.generateAndStoreEmbedding(alert);
            }
        } catch (Exception e) {
            log.warn("报警向量嵌入生成失败: alertId={}, error={}", alertId, e.getMessage());
        }
    }

    private AlertAnalysisService resolveService(String alertType) {
        if (alertType == null) {
            return analysisServices.get("drowningAlertAnalysisService");
        }
        return switch (alertType.toUpperCase()) {
            case "DROWING", "DROWNING" -> analysisServices.get("drowningAlertAnalysisService");
            case "CROSS_BORDER" -> analysisServices.get("invasionAlertAnalysisService");
            default -> analysisServices.get("drowningAlertAnalysisService");
        };
    }
}
