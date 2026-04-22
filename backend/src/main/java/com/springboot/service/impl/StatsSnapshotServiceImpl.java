package com.springboot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.StatsSnapshot;
import com.springboot.model.dto.statssnapshot.StatsSnapshotQueryRequest;
import com.springboot.model.vo.StatsSnapshotVO;
import com.springboot.service.StatsSnapshotService;
import com.springboot.mapper.StatsSnapshotMapper;
import com.springboot.utils.SqlUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @description 针对表【stats_snapshot(统计快照表)】的数据库操作Service实现
*/
@Service
public class StatsSnapshotServiceImpl extends ServiceImpl<StatsSnapshotMapper, StatsSnapshot>
    implements StatsSnapshotService{

    @Override
    public void validStatsSnapshot(StatsSnapshot statsSnapshot, boolean add) {
        if (statsSnapshot == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "统计快照不能为空");
        }
        if (add && StringUtils.isBlank(statsSnapshot.getGranularity())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "粒度不能为空");
        }
        if (add && statsSnapshot.getSnapshot_date() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "快照日期不能为空");
        }
        if (add && StringUtils.isBlank(statsSnapshot.getMetric_type())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "指标类型不能为空");
        }
        if (add && StringUtils.isBlank(statsSnapshot.getMetric_key())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "指标键不能为空");
        }
    }

    @Override
    public QueryWrapper<StatsSnapshot> getQueryWrapper(StatsSnapshotQueryRequest statsSnapshotQueryRequest) {
        if (statsSnapshotQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<StatsSnapshot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(statsSnapshotQueryRequest.getId() != null, "id", statsSnapshotQueryRequest.getId());
        queryWrapper.eq(StringUtils.isNotBlank(statsSnapshotQueryRequest.getGranularity()), "granularity",
                statsSnapshotQueryRequest.getGranularity());
        queryWrapper.eq(statsSnapshotQueryRequest.getSnapshotDate() != null, "snapshot_date", statsSnapshotQueryRequest.getSnapshotDate());
        queryWrapper.eq(statsSnapshotQueryRequest.getSnapshotHour() != null, "snapshot_hour", statsSnapshotQueryRequest.getSnapshotHour());
        queryWrapper.eq(statsSnapshotQueryRequest.getVenueId() != null, "venue_id", statsSnapshotQueryRequest.getVenueId());
        queryWrapper.eq(StringUtils.isNotBlank(statsSnapshotQueryRequest.getMetricType()), "metric_type",
                statsSnapshotQueryRequest.getMetricType());
        queryWrapper.eq(StringUtils.isNotBlank(statsSnapshotQueryRequest.getMetricKey()), "metric_key",
                statsSnapshotQueryRequest.getMetricKey());
        String sortField = statsSnapshotQueryRequest.getSortField();
        String sortOrder = statsSnapshotQueryRequest.getSortOrder();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public StatsSnapshotVO getStatsSnapshotVO(StatsSnapshot statsSnapshot) {
        if (statsSnapshot == null) {
            return null;
        }
        StatsSnapshotVO statsSnapshotVO = new StatsSnapshotVO();
        statsSnapshotVO.setId(statsSnapshot.getId());
        statsSnapshotVO.setGranularity(statsSnapshot.getGranularity());
        statsSnapshotVO.setSnapshotDate(statsSnapshot.getSnapshot_date());
        statsSnapshotVO.setSnapshotHour(statsSnapshot.getSnapshot_hour());
        statsSnapshotVO.setVenueId(statsSnapshot.getVenue_id());
        statsSnapshotVO.setMetricType(statsSnapshot.getMetric_type());
        statsSnapshotVO.setMetricKey(statsSnapshot.getMetric_key());
        statsSnapshotVO.setMetricValue(statsSnapshot.getMetric_value());
        statsSnapshotVO.setDimensionJson(statsSnapshot.getDimension_json());
        statsSnapshotVO.setCreatedAt(statsSnapshot.getCreated_at());
        return statsSnapshotVO;
    }

    @Override
    public List<StatsSnapshotVO> getStatsSnapshotVO(List<StatsSnapshot> statsSnapshotList) {
        if (CollUtil.isEmpty(statsSnapshotList)) {
            return new ArrayList<>();
        }
        return statsSnapshotList.stream().map(this::getStatsSnapshotVO).collect(Collectors.toList());
    }
}




