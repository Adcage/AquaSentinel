package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.CameraMaintenanceLog;
import com.springboot.model.dto.cameramaintenancelog.CameraMaintenanceLogQueryRequest;
import com.springboot.model.vo.CameraMaintenanceLogVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【camera_maintenance_log(设备维护记录表)】的数据库操作Service
*/
public interface CameraMaintenanceLogService extends IService<CameraMaintenanceLog> {

    void validCameraMaintenanceLog(CameraMaintenanceLog cameraMaintenanceLog, boolean add);

    QueryWrapper<CameraMaintenanceLog> getQueryWrapper(CameraMaintenanceLogQueryRequest cameraMaintenanceLogQueryRequest);

    CameraMaintenanceLogVO getCameraMaintenanceLogVO(CameraMaintenanceLog cameraMaintenanceLog);

    List<CameraMaintenanceLogVO> getCameraMaintenanceLogVO(List<CameraMaintenanceLog> cameraMaintenanceLogList);
}
