package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.CameraDeviceMapper;
import com.springboot.model.dto.cameradevice.CameraDeviceQueryRequest;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.vo.CameraDeviceVO;
import com.springboot.service.CameraDeviceService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【camera_device(摄像头设备表)】的数据库操作Service实现
 */
@Service
public class CameraDeviceServiceImpl extends ServiceImpl<CameraDeviceMapper, CameraDevice>
        implements CameraDeviceService {

    @Override
    public void validCameraDevice(CameraDevice cameraDevice, boolean add) {
        if (cameraDevice == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头信息不能为空");
        }
        if (add && cameraDevice.getVenue_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆ID不能为空");
        }
        if (add && StringUtils.isBlank(cameraDevice.getCamera_code())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头编码不能为空");
        }
        if (add && StringUtils.isBlank(cameraDevice.getCamera_name())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头名称不能为空");
        }
        if (add && StringUtils.isBlank(cameraDevice.getStream_url())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "视频流地址不能为空");
        }
        if (StringUtils.isNotBlank(cameraDevice.getCamera_code())
                && cameraDevice.getCamera_code().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头编码过长");
        }
        if (StringUtils.isNotBlank(cameraDevice.getCamera_name())
                && cameraDevice.getCamera_name().length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头名称过长");
        }
        if (StringUtils.isNotBlank(cameraDevice.getStream_url())
                && cameraDevice.getStream_url().length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "视频流地址过长");
        }
        if (StringUtils.isNotBlank(cameraDevice.getProtocol())
                && cameraDevice.getProtocol().length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "流协议过长");
        }
        if (StringUtils.isNotBlank(cameraDevice.getDevice_status())
                && cameraDevice.getDevice_status().length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备状态值过长");
        }
        if (StringUtils.isNotBlank(cameraDevice.getHealth_status())
                && cameraDevice.getHealth_status().length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "健康状态值过长");
        }
        if (cameraDevice.getEnabled() != null
                && !Objects.equals(cameraDevice.getEnabled(), 0)
                && !Objects.equals(cameraDevice.getEnabled(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "启用状态无效");
        }
        if (StringUtils.isNotBlank(cameraDevice.getCamera_code())) {
            QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("camera_code", cameraDevice.getCamera_code());
            queryWrapper.eq("is_delete", 0);
            queryWrapper.ne(cameraDevice.getId() != null, "id", cameraDevice.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头编码已存在");
            }
        }
    }

    @Override
    public QueryWrapper<CameraDevice> getQueryWrapper(
            CameraDeviceQueryRequest cameraDeviceQueryRequest) {
        if (cameraDeviceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(
                cameraDeviceQueryRequest.getId() != null, "id", cameraDeviceQueryRequest.getId());
        queryWrapper.eq(
                cameraDeviceQueryRequest.getVenueId() != null,
                "venue_id",
                cameraDeviceQueryRequest.getVenueId());
        queryWrapper.eq(
                cameraDeviceQueryRequest.getZoneId() != null,
                "zone_id",
                cameraDeviceQueryRequest.getZoneId());
        queryWrapper.eq(
                StringUtils.isNotBlank(cameraDeviceQueryRequest.getCameraCode()),
                "camera_code",
                cameraDeviceQueryRequest.getCameraCode());
        queryWrapper.like(
                StringUtils.isNotBlank(cameraDeviceQueryRequest.getCameraName()),
                "camera_name",
                cameraDeviceQueryRequest.getCameraName());
        queryWrapper.like(
                StringUtils.isNotBlank(cameraDeviceQueryRequest.getStreamUrl()),
                "stream_url",
                cameraDeviceQueryRequest.getStreamUrl());
        queryWrapper.eq(
                StringUtils.isNotBlank(cameraDeviceQueryRequest.getProtocol()),
                "protocol",
                cameraDeviceQueryRequest.getProtocol());
        queryWrapper.eq(
                StringUtils.isNotBlank(cameraDeviceQueryRequest.getDeviceStatus()),
                "device_status",
                cameraDeviceQueryRequest.getDeviceStatus());
        queryWrapper.eq(
                StringUtils.isNotBlank(cameraDeviceQueryRequest.getHealthStatus()),
                "health_status",
                cameraDeviceQueryRequest.getHealthStatus());
        queryWrapper.eq(
                cameraDeviceQueryRequest.getEnabled() != null,
                "enabled",
                cameraDeviceQueryRequest.getEnabled());
        queryWrapper.eq("is_delete", 0);
        String sortField = cameraDeviceQueryRequest.getSortField();
        String sortOrder = cameraDeviceQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public CameraDeviceVO getCameraDeviceVO(CameraDevice cameraDevice) {
        if (cameraDevice == null) {
            return null;
        }
        CameraDeviceVO cameraDeviceVO = new CameraDeviceVO();
        cameraDeviceVO.setId(cameraDevice.getId());
        cameraDeviceVO.setVenueId(cameraDevice.getVenue_id());
        cameraDeviceVO.setZoneId(cameraDevice.getZone_id());
        cameraDeviceVO.setCameraCode(cameraDevice.getCamera_code());
        cameraDeviceVO.setCameraName(cameraDevice.getCamera_name());
        cameraDeviceVO.setStreamUrl(cameraDevice.getStream_url());
        cameraDeviceVO.setProtocol(cameraDevice.getProtocol());
        cameraDeviceVO.setDeviceStatus(cameraDevice.getDevice_status());
        cameraDeviceVO.setHealthStatus(cameraDevice.getHealth_status());
        cameraDeviceVO.setEnabled(cameraDevice.getEnabled());
        cameraDeviceVO.setLastHeartbeatAt(cameraDevice.getLast_heartbeat_at());
        cameraDeviceVO.setCreatedAt(cameraDevice.getCreated_at());
        cameraDeviceVO.setUpdatedAt(cameraDevice.getUpdated_at());
        return cameraDeviceVO;
    }

    @Override
    public List<CameraDeviceVO> getCameraDeviceVO(List<CameraDevice> cameraDeviceList) {
        if (CollUtil.isEmpty(cameraDeviceList)) {
            return new ArrayList<>();
        }
        return cameraDeviceList.stream().map(this::getCameraDeviceVO).collect(Collectors.toList());
    }
}
