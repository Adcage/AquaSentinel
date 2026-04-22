package com.springboot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationLogQueryRequest;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.vo.LifeguardLocationLogVO;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.mapper.LifeguardLocationLogMapper;
import com.springboot.utils.SqlUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @description 针对表【lifeguard_location_log(救生员定位上报表)】的数据库操作Service实现
*/
@Service
public class LifeguardLocationLogServiceImpl extends ServiceImpl<LifeguardLocationLogMapper, LifeguardLocationLog>
    implements LifeguardLocationLogService{

    @Override
    public void validLifeguardLocationLog(LifeguardLocationLog lifeguardLocationLog, boolean add) {
        if (lifeguardLocationLog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "定位日志不能为空");
        }
        if (add && lifeguardLocationLog.getLifeguard_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员ID不能为空");
        }
        if (add && lifeguardLocationLog.getVenue_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆ID不能为空");
        }
        if (add && lifeguardLocationLog.getLongitude() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "经度不能为空");
        }
        if (add && lifeguardLocationLog.getLatitude() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "纬度不能为空");
        }
    }

    @Override
    public QueryWrapper<LifeguardLocationLog> getQueryWrapper(LifeguardLocationLogQueryRequest lifeguardLocationLogQueryRequest) {
        if (lifeguardLocationLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<LifeguardLocationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(lifeguardLocationLogQueryRequest.getId() != null, "id", lifeguardLocationLogQueryRequest.getId());
        queryWrapper.eq(lifeguardLocationLogQueryRequest.getLifeguardId() != null,
                "lifeguard_id", lifeguardLocationLogQueryRequest.getLifeguardId());
        queryWrapper.eq(lifeguardLocationLogQueryRequest.getVenueId() != null,
                "venue_id", lifeguardLocationLogQueryRequest.getVenueId());
        queryWrapper.eq(lifeguardLocationLogQueryRequest.getInFence() != null,
                "in_fence", lifeguardLocationLogQueryRequest.getInFence());
        queryWrapper.eq(StringUtils.isNotBlank(lifeguardLocationLogQueryRequest.getReportSource()),
                "report_source", lifeguardLocationLogQueryRequest.getReportSource());
        String sortField = lifeguardLocationLogQueryRequest.getSortField();
        String sortOrder = lifeguardLocationLogQueryRequest.getSortOrder();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public LifeguardLocationLogVO getLifeguardLocationLogVO(LifeguardLocationLog lifeguardLocationLog) {
        if (lifeguardLocationLog == null) {
            return null;
        }
        LifeguardLocationLogVO lifeguardLocationLogVO = new LifeguardLocationLogVO();
        lifeguardLocationLogVO.setId(lifeguardLocationLog.getId());
        lifeguardLocationLogVO.setLifeguardId(lifeguardLocationLog.getLifeguard_id());
        lifeguardLocationLogVO.setVenueId(lifeguardLocationLog.getVenue_id());
        lifeguardLocationLogVO.setLongitude(lifeguardLocationLog.getLongitude());
        lifeguardLocationLogVO.setLatitude(lifeguardLocationLog.getLatitude());
        lifeguardLocationLogVO.setInFence(lifeguardLocationLog.getIn_fence());
        lifeguardLocationLogVO.setReportSource(lifeguardLocationLog.getReport_source());
        lifeguardLocationLogVO.setReportedAt(lifeguardLocationLog.getReported_at());
        return lifeguardLocationLogVO;
    }

    @Override
    public List<LifeguardLocationLogVO> getLifeguardLocationLogVO(List<LifeguardLocationLog> lifeguardLocationLogList) {
        if (CollUtil.isEmpty(lifeguardLocationLogList)) {
            return new ArrayList<>();
        }
        return lifeguardLocationLogList.stream().map(this::getLifeguardLocationLogVO).collect(Collectors.toList());
    }

    @Override
    public boolean reportLocation(LifeguardLocationLog lifeguardLocationLog) {
        validLifeguardLocationLog(lifeguardLocationLog, true);
        lifeguardLocationLog.setIn_fence(lifeguardLocationLog.getIn_fence() == null ? 1 : lifeguardLocationLog.getIn_fence());
        lifeguardLocationLog.setReport_source(StringUtils.defaultIfBlank(lifeguardLocationLog.getReport_source(), "APP"));
        lifeguardLocationLog.setReported_at(lifeguardLocationLog.getReported_at() == null ? new Date() : lifeguardLocationLog.getReported_at());
        return this.save(lifeguardLocationLog);
    }

    @Override
    public List<LifeguardLocationLog> recentLocations(Long lifeguardId, Integer limit) {
        if (lifeguardId == null || lifeguardId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员ID错误");
        }
        int finalLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 100);
        QueryWrapper<LifeguardLocationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lifeguard_id", lifeguardId);
        queryWrapper.orderByDesc("reported_at", "id");
        queryWrapper.last("limit " + finalLimit);
        return this.list(queryWrapper);
    }

    @Override
    public boolean detectOffPost(Long lifeguardId) {
        List<LifeguardLocationLog> recentLocations = recentLocations(lifeguardId, 1);
        if (CollUtil.isEmpty(recentLocations)) {
            return true;
        }
        LifeguardLocationLog latest = recentLocations.get(0);
        return latest.getIn_fence() == null || latest.getIn_fence() == 0;
    }

}




