package com.springboot.ai.embedding;

import java.util.List;

/** 报警向量仓库接口 */
public interface AlertVectorRepository {

    /** 存储报警向量 */
    void store(Long alertId, String alertUid, String sourceText, float[] embedding, String model);

    /** 获取报警向量 */
    float[] getEmbedding(Long alertId);

    /** 搜索相似报警 */
    List<SimilarAlert> searchSimilar(float[] queryEmbedding, int topK, double threshold);

    /** 删除报警向量 */
    void delete(Long alertId);

    record SimilarAlert(Long alertId, String alertUid, double similarity) {}
}
