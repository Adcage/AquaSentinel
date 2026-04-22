package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.dto.alertrecord.AlertRecordQueryRequest;
import com.springboot.model.vo.AlertRecordVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【alert_record(报警记录表)】的数据库操作Service
*/
public interface AlertRecordService extends IService<AlertRecord> {

    void validAlertRecord(AlertRecord alertRecord, boolean add);

    QueryWrapper<AlertRecord> getQueryWrapper(AlertRecordQueryRequest alertRecordQueryRequest);

    AlertRecordVO getAlertRecordVO(AlertRecord alertRecord);

    List<AlertRecordVO> getAlertRecordVO(List<AlertRecord> alertRecordList);
}
