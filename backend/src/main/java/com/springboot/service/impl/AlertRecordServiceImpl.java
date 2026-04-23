package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.AlertRecordMapper;
import com.springboot.model.dto.alertrecord.AlertRecordQueryRequest;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.vo.AlertRecordVO;
import com.springboot.service.AlertRecordService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【alert_record(报警记录表)】的数据库操作Service实现
 */
@Service
public class AlertRecordServiceImpl extends ServiceImpl<AlertRecordMapper, AlertRecord>
        implements AlertRecordService {

    @Override
    public void validAlertRecord(AlertRecord alertRecord, boolean add) {
        if (alertRecord == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "报警记录不能为空");
        }
        if (add && StringUtils.isBlank(alertRecord.getAlert_uid())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "报警编码不能为空");
        }
        if (add && alertRecord.getEvent_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "事件ID不能为空");
        }
        if (add && alertRecord.getCamera_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头ID不能为空");
        }
        if (add && alertRecord.getVenue_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆ID不能为空");
        }
        if (StringUtils.isNotBlank(alertRecord.getAlert_uid())) {
            QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("alert_uid", alertRecord.getAlert_uid());
            queryWrapper.ne(alertRecord.getId() != null, "id", alertRecord.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "报警编码已存在");
            }
        }
    }

    @Override
    public QueryWrapper<AlertRecord> getQueryWrapper(
            AlertRecordQueryRequest alertRecordQueryRequest) {
        if (alertRecordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(
                alertRecordQueryRequest.getId() != null, "id", alertRecordQueryRequest.getId());
        queryWrapper.eq(
                StringUtils.isNotBlank(alertRecordQueryRequest.getAlertUid()),
                "alert_uid",
                alertRecordQueryRequest.getAlertUid());
        queryWrapper.eq(
                alertRecordQueryRequest.getEventId() != null,
                "event_id",
                alertRecordQueryRequest.getEventId());
        queryWrapper.eq(
                alertRecordQueryRequest.getCameraId() != null,
                "camera_id",
                alertRecordQueryRequest.getCameraId());
        queryWrapper.eq(
                alertRecordQueryRequest.getVenueId() != null,
                "venue_id",
                alertRecordQueryRequest.getVenueId());
        queryWrapper.eq(
                alertRecordQueryRequest.getLifeguardId() != null,
                "lifeguard_id",
                alertRecordQueryRequest.getLifeguardId());
        queryWrapper.eq(
                StringUtils.isNotBlank(alertRecordQueryRequest.getAlertType()),
                "alert_type",
                alertRecordQueryRequest.getAlertType());
        queryWrapper.eq(
                StringUtils.isNotBlank(alertRecordQueryRequest.getAlertStatus()),
                "alert_status",
                alertRecordQueryRequest.getAlertStatus());
        queryWrapper.ge(
                alertRecordQueryRequest.getStartCreatedAt() != null,
                "created_at",
                alertRecordQueryRequest.getStartCreatedAt());
        queryWrapper.le(
                alertRecordQueryRequest.getEndCreatedAt() != null,
                "created_at",
                alertRecordQueryRequest.getEndCreatedAt());
        queryWrapper.ge(
                alertRecordQueryRequest.getStartCreatedAt() == null
                        && alertRecordQueryRequest.getStartTime() != null,
                "created_at",
                alertRecordQueryRequest.getStartTime());
        queryWrapper.le(
                alertRecordQueryRequest.getEndCreatedAt() == null
                        && alertRecordQueryRequest.getEndTime() != null,
                "created_at",
                alertRecordQueryRequest.getEndTime());
        if (StringUtils.isNotBlank(alertRecordQueryRequest.getKeyword())) {
            queryWrapper.and(
                    wrapper ->
                            wrapper.like("incident_location", alertRecordQueryRequest.getKeyword())
                                    .or()
                                    .like(
                                            "emergency_contact_name",
                                            alertRecordQueryRequest.getKeyword())
                                    .or()
                                    .like("alert_uid", alertRecordQueryRequest.getKeyword()));
        }
        String sortField = alertRecordQueryRequest.getSortField();
        String sortOrder = alertRecordQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public AlertRecordVO getAlertRecordVO(AlertRecord alertRecord) {
        if (alertRecord == null) {
            return null;
        }
        AlertRecordVO alertRecordVO = new AlertRecordVO();
        alertRecordVO.setId(alertRecord.getId());
        alertRecordVO.setAlertUid(alertRecord.getAlert_uid());
        alertRecordVO.setEventId(alertRecord.getEvent_id());
        alertRecordVO.setCameraId(alertRecord.getCamera_id());
        alertRecordVO.setVenueId(alertRecord.getVenue_id());
        alertRecordVO.setLifeguardId(alertRecord.getLifeguard_id());
        alertRecordVO.setAlertType(alertRecord.getAlert_type());
        alertRecordVO.setAlertStatus(alertRecord.getAlert_status());
        alertRecordVO.setEmergencyContactName(alertRecord.getEmergency_contact_name());
        alertRecordVO.setEmergencyContactPhone(alertRecord.getEmergency_contact_phone());
        alertRecordVO.setIncidentLocation(alertRecord.getIncident_location());
        alertRecordVO.setVideoStreamUrl(alertRecord.getVideo_stream_url());
        alertRecordVO.setDetectionResult(alertRecord.getDetection_result());
        alertRecordVO.setPushedToApp(alertRecord.getPushed_to_app());
        alertRecordVO.setPushedToPc(alertRecord.getPushed_to_pc());
        alertRecordVO.setFirstPushTime(alertRecord.getFirst_push_time());
        alertRecordVO.setResolvedTime(alertRecord.getResolved_time());
        alertRecordVO.setCreatedAt(alertRecord.getCreated_at());
        alertRecordVO.setUpdatedAt(alertRecord.getUpdated_at());
        return alertRecordVO;
    }

    @Override
    public List<AlertRecordVO> getAlertRecordVO(List<AlertRecord> alertRecordList) {
        if (CollUtil.isEmpty(alertRecordList)) {
            return new ArrayList<>();
        }
        return alertRecordList.stream().map(this::getAlertRecordVO).collect(Collectors.toList());
    }
}
