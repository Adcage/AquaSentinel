package com.springboot.ai.analysis;

/** 报警分析服务接口 */
public interface AlertAnalysisService {

    /**
     * 分析报警并生成增强描述
     *
     * @param alertId 报警ID
     * @return AI 分析结果文本，null 表示分析失败
     */
    String analyzeAlert(Long alertId);

    /** 获取服务支持的报警类型 */
    String getSupportedAlertType();
}
