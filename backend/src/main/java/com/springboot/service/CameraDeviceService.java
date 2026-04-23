package com.springboot.service;

import java.util.List;

import com.springboot.model.dto.cameradevice.CameraDeviceQueryRequest;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.vo.CameraDeviceVO;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【camera_device(摄像头设备表)】的数据库操作Service
 */
public interface CameraDeviceService extends IService<CameraDevice> {

    void validCameraDevice(CameraDevice cameraDevice, boolean add);

    QueryWrapper<CameraDevice> getQueryWrapper(CameraDeviceQueryRequest cameraDeviceQueryRequest);

    CameraDeviceVO getCameraDeviceVO(CameraDevice cameraDevice);

    List<CameraDeviceVO> getCameraDeviceVO(List<CameraDevice> cameraDeviceList);
}
