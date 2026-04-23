package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.CameraMaintenanceLogMapper;
import com.springboot.model.dto.cameramaintenancelog.CameraMaintenanceLogQueryRequest;
import com.springboot.model.entity.CameraMaintenanceLog;
import com.springboot.model.vo.CameraMaintenanceLogVO;
import com.springboot.service.CameraMaintenanceLogService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【camera_maintenance_log(设备维护记录表)】的数据库操作Service实现
 */
@Service
public class CameraMaintenanceLogServiceImpl
        extends ServiceImpl<CameraMaintenanceLogMapper, CameraMaintenanceLog>
        implements CameraMaintenanceLogService {

    @Override
    public void validCameraMaintenanceLog(CameraMaintenanceLog cameraMaintenanceLog, boolean add) {
        if (cameraMaintenanceLog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护记录不能为空");
        }
        if (add && cameraMaintenanceLog.getCamera_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头ID不能为空");
        }
        if (add && StringUtils.isBlank(cameraMaintenanceLog.getMaintenance_type())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护类型不能为空");
        }
        if (add && StringUtils.isBlank(cameraMaintenanceLog.getMaintenance_content())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护内容不能为空");
        }
        if (add && StringUtils.isBlank(cameraMaintenanceLog.getMaintained_by())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护人不能为空");
        }
        if (StringUtils.isNotBlank(cameraMaintenanceLog.getMaintenance_type())
                && cameraMaintenanceLog.getMaintenance_type().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护类型过长");
        }
        if (StringUtils.isNotBlank(cameraMaintenanceLog.getMaintenance_content())
                && cameraMaintenanceLog.getMaintenance_content().length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护内容过长");
        }
        if (StringUtils.isNotBlank(cameraMaintenanceLog.getMaintained_by())
                && cameraMaintenanceLog.getMaintained_by().length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "维护人名称过长");
        }
        if (cameraMaintenanceLog.getMaintained_at() != null
                && cameraMaintenanceLog.getNext_maintenance_at() != null
                && cameraMaintenanceLog
                        .getNext_maintenance_at()
                        .before(cameraMaintenanceLog.getMaintained_at())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "下次维护时间不能早于维护时间");
        }
        if (cameraMaintenanceLog.getCamera_id() != null
                && StringUtils.isNotBlank(cameraMaintenanceLog.getMaintenance_type())
                && cameraMaintenanceLog.getMaintained_at() != null) {
            QueryWrapper<CameraMaintenanceLog> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("camera_id", cameraMaintenanceLog.getCamera_id());
            queryWrapper.eq("maintenance_type", cameraMaintenanceLog.getMaintenance_type());
            queryWrapper.eq("maintained_at", cameraMaintenanceLog.getMaintained_at());
            queryWrapper.ne(
                    cameraMaintenanceLog.getId() != null, "id", cameraMaintenanceLog.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "同一维护记录已存在");
            }
        }
    }

    @Override
    public QueryWrapper<CameraMaintenanceLog> getQueryWrapper(
            CameraMaintenanceLogQueryRequest cameraMaintenanceLogQueryRequest) {
        if (cameraMaintenanceLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<CameraMaintenanceLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(
                cameraMaintenanceLogQueryRequest.getId() != null,
                "id",
                cameraMaintenanceLogQueryRequest.getId());
        queryWrapper.eq(
                cameraMaintenanceLogQueryRequest.getCameraId() != null,
                "camera_id",
                cameraMaintenanceLogQueryRequest.getCameraId());
        queryWrapper.eq(
                StringUtils.isNotBlank(cameraMaintenanceLogQueryRequest.getMaintenanceType()),
                "maintenance_type",
                cameraMaintenanceLogQueryRequest.getMaintenanceType());
        queryWrapper.like(
                StringUtils.isNotBlank(cameraMaintenanceLogQueryRequest.getMaintenanceContent()),
                "maintenance_content",
                cameraMaintenanceLogQueryRequest.getMaintenanceContent());
        queryWrapper.like(
                StringUtils.isNotBlank(cameraMaintenanceLogQueryRequest.getMaintainedBy()),
                "maintained_by",
                cameraMaintenanceLogQueryRequest.getMaintainedBy());
        queryWrapper.ge(
                cameraMaintenanceLogQueryRequest.getStartMaintainedAt() != null,
                "maintained_at",
                cameraMaintenanceLogQueryRequest.getStartMaintainedAt());
        queryWrapper.le(
                cameraMaintenanceLogQueryRequest.getEndMaintainedAt() != null,
                "maintained_at",
                cameraMaintenanceLogQueryRequest.getEndMaintainedAt());
        String sortField = cameraMaintenanceLogQueryRequest.getSortField();
        String sortOrder = cameraMaintenanceLogQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public CameraMaintenanceLogVO getCameraMaintenanceLogVO(
            CameraMaintenanceLog cameraMaintenanceLog) {
        if (cameraMaintenanceLog == null) {
            return null;
        }
        CameraMaintenanceLogVO cameraMaintenanceLogVO = new CameraMaintenanceLogVO();
        cameraMaintenanceLogVO.setId(cameraMaintenanceLog.getId());
        cameraMaintenanceLogVO.setCameraId(cameraMaintenanceLog.getCamera_id());
        cameraMaintenanceLogVO.setMaintenanceType(cameraMaintenanceLog.getMaintenance_type());
        cameraMaintenanceLogVO.setMaintenanceContent(cameraMaintenanceLog.getMaintenance_content());
        cameraMaintenanceLogVO.setMaintainedBy(cameraMaintenanceLog.getMaintained_by());
        cameraMaintenanceLogVO.setMaintainedAt(cameraMaintenanceLog.getMaintained_at());
        cameraMaintenanceLogVO.setNextMaintenanceAt(cameraMaintenanceLog.getNext_maintenance_at());
        return cameraMaintenanceLogVO;
    }

    @Override
    public List<CameraMaintenanceLogVO> getCameraMaintenanceLogVO(
            List<CameraMaintenanceLog> cameraMaintenanceLogList) {
        if (CollUtil.isEmpty(cameraMaintenanceLogList)) {
            return new ArrayList<>();
        }
        return cameraMaintenanceLogList.stream()
                .map(this::getCameraMaintenanceLogVO)
                .collect(Collectors.toList());
    }
}
