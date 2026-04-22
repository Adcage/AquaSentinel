package com.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.AiStreamTaskMapper;
import com.springboot.mapper.SystemNoticeConfigMapper;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.SystemNoticeConfig;
import com.springboot.model.vo.NoticeSettingsVO;
import com.springboot.service.AiEngineClient;
import com.springboot.service.SystemNoticeConfigService;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemNoticeConfigServiceImpl extends ServiceImpl<SystemNoticeConfigMapper, SystemNoticeConfig>
        implements SystemNoticeConfigService {

    private static final int DEFAULT_OFF_DUTY_THRESHOLD_SEC = 60;

    private static final int DEFAULT_DEVICE_OFFLINE_THRESHOLD_SEC = 180;

    private static final int DEFAULT_DROWNING_ALERT_THRESHOLD_SEC = 3;

    private static final int MAX_THRESHOLD_SEC = 3600;

    private volatile NoticeSettingsVO cachedSettings;

    @Resource
    private AiStreamTaskMapper aiStreamTaskMapper;

    @Resource
    private AiEngineClient aiEngineClient;

    @Override
    public NoticeSettingsVO getNoticeSettings() {
        NoticeSettingsVO cached = cachedSettings;
        if (cached != null) {
            return cached;
        }

        QueryWrapper<SystemNoticeConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.last("limit 1");
        SystemNoticeConfig config;
        try {
            config = this.getOne(queryWrapper);
        } catch (Exception ex) {
            config = null;
        }
        NoticeSettingsVO result = toNoticeSettingsVO(config);
        cachedSettings = result;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeSettingsVO updateNoticeSettings(Integer offDutyThresholdSec,
            Integer deviceOfflineThresholdSec, Integer drowningAlertThresholdSec) {
        validateThreshold(offDutyThresholdSec, "脱岗告警阈值");
        validateThreshold(deviceOfflineThresholdSec, "设备离线阈值");
        validateThreshold(drowningAlertThresholdSec, "溺水持续判定阈值");

        QueryWrapper<SystemNoticeConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.last("limit 1");
        SystemNoticeConfig config = this.getOne(queryWrapper);
        Date now = new Date();
        if (config == null) {
            config = new SystemNoticeConfig();
            config.setCreated_at(now);
        }
        config.setOff_duty_threshold_sec(offDutyThresholdSec);
        config.setDevice_offline_threshold_sec(deviceOfflineThresholdSec);
        config.setDrowning_alert_threshold_sec(drowningAlertThresholdSec);
        config.setUpdated_at(now);

        boolean ok = this.saveOrUpdate(config);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "通知配置保存失败");
        }

        NoticeSettingsVO result = toNoticeSettingsVO(config);
        cachedSettings = result;
        syncRunningAiTasks(result.getDrowningAlertThreshold());
        return result;
    }

    @Override
    public int getOffDutyThresholdSec() {
        return getNoticeSettings().getOffDutyThreshold();
    }

    @Override
    public int getDeviceOfflineThresholdSec() {
        return getNoticeSettings().getDeviceOfflineThreshold();
    }

    @Override
    public int getDrowningAlertThresholdSec() {
        return getNoticeSettings().getDrowningAlertThreshold();
    }

    private NoticeSettingsVO toNoticeSettingsVO(SystemNoticeConfig config) {
        NoticeSettingsVO vo = new NoticeSettingsVO();
        vo.setOffDutyThreshold(config == null || config.getOff_duty_threshold_sec() == null
                ? DEFAULT_OFF_DUTY_THRESHOLD_SEC : config.getOff_duty_threshold_sec());
        vo.setDeviceOfflineThreshold(config == null || config.getDevice_offline_threshold_sec() == null
                ? DEFAULT_DEVICE_OFFLINE_THRESHOLD_SEC : config.getDevice_offline_threshold_sec());
        vo.setDrowningAlertThreshold(config == null || config.getDrowning_alert_threshold_sec() == null
                ? DEFAULT_DROWNING_ALERT_THRESHOLD_SEC : config.getDrowning_alert_threshold_sec());
        return vo;
    }

    private void syncRunningAiTasks(Integer drowningAlertThresholdSec) {
        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("task_status", "RUNNING", "STARTING");
        List<AiStreamTask> runningTasks = aiStreamTaskMapper.selectList(queryWrapper);
        for (AiStreamTask task : runningTasks) {
            if (task == null || task.getCamera_id() == null || task.getCamera_id() <= 0) {
                continue;
            }
            try {
                aiEngineClient.updateTaskConfig(task.getTask_code(), drowningAlertThresholdSec.doubleValue());
            } catch (Exception ignored) {
            }
        }
    }

    private void validateThreshold(Integer value, String fieldName) {
        if (value == null || value <= 0 || value > MAX_THRESHOLD_SEC) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "取值范围应在1~" + MAX_THRESHOLD_SEC + "秒");
        }
    }

}
