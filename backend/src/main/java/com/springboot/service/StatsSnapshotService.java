package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.StatsSnapshot;
import com.springboot.model.dto.statssnapshot.StatsSnapshotQueryRequest;
import com.springboot.model.vo.StatsSnapshotVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【stats_snapshot(统计快照表)】的数据库操作Service
*/
public interface StatsSnapshotService extends IService<StatsSnapshot> {

    void validStatsSnapshot(StatsSnapshot statsSnapshot, boolean add);

    QueryWrapper<StatsSnapshot> getQueryWrapper(StatsSnapshotQueryRequest statsSnapshotQueryRequest);

    StatsSnapshotVO getStatsSnapshotVO(StatsSnapshot statsSnapshot);

    List<StatsSnapshotVO> getStatsSnapshotVO(List<StatsSnapshot> statsSnapshotList);
}
