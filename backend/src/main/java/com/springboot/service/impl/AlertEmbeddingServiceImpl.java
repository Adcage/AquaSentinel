package com.springboot.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.springboot.mapper.AlertEmbeddingMapper;
import com.springboot.model.entity.AlertEmbedding;
import com.springboot.service.AlertEmbeddingService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** 报警向量嵌入服务实现 */
@Service
public class AlertEmbeddingServiceImpl implements AlertEmbeddingService {

    @Resource private AlertEmbeddingMapper embeddingMapper;

    @Override
    public void saveEmbedding(AlertEmbedding embedding) {
        embeddingMapper.insert(embedding);
    }

    @Override
    public AlertEmbedding getByAlertId(Long alertId) {
        QueryWrapper<AlertEmbedding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alert_id", alertId);
        queryWrapper.eq("is_delete", 0);
        return embeddingMapper.selectOne(queryWrapper);
    }

    @Override
    public AlertEmbedding getByAlertUid(String alertUid) {
        QueryWrapper<AlertEmbedding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alert_uid", alertUid);
        queryWrapper.eq("is_delete", 0);
        return embeddingMapper.selectOne(queryWrapper);
    }

    @Override
    public void updateEmbedding(Long alertId, String embedding) {
        AlertEmbedding existing = getByAlertId(alertId);
        if (existing == null) {
            return;
        }
        AlertEmbedding update = new AlertEmbedding();
        update.setId(existing.getId());
        update.setEmbedding(embedding);
        update.setUpdated_at(new Date());
        embeddingMapper.updateById(update);
    }

    @Override
    public List<AlertEmbedding> listRecentEmbeddings(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        Date startDate = calendar.getTime();

        QueryWrapper<AlertEmbedding> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.ge("created_at", startDate);
        queryWrapper.orderByDesc("created_at");
        return embeddingMapper.selectList(queryWrapper);
    }

    @Override
    public void saveBatch(List<AlertEmbedding> embeddings) {
        for (AlertEmbedding embedding : embeddings) {
            embeddingMapper.insert(embedding);
        }
    }
}
