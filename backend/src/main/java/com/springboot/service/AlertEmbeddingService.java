package com.springboot.service;

import java.util.List;

import com.springboot.model.entity.AlertEmbedding;

/** 报警向量嵌入服务接口 */
public interface AlertEmbeddingService {

    /** 保存嵌入记录 */
    void saveEmbedding(AlertEmbedding embedding);

    /** 按alertId查询 */
    AlertEmbedding getByAlertId(Long alertId);

    /** 按alertUid查询 */
    AlertEmbedding getByAlertUid(String alertUid);

    /** 更新嵌入向量 */
    void updateEmbedding(Long alertId, String embedding);

    /** 查询最近N天的嵌入记录 */
    List<AlertEmbedding> listRecentEmbeddings(int days);

    /** 批量保存嵌入记录 */
    void saveBatch(List<AlertEmbedding> embeddings);
}
