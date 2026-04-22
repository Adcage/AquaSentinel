package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.AlertDisposal;
import com.springboot.model.dto.alertdisposal.AlertDisposalQueryRequest;
import com.springboot.model.vo.AlertDisposalVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【alert_disposal(报警处置表)】的数据库操作Service
*/
public interface AlertDisposalService extends IService<AlertDisposal> {

    void validAlertDisposal(AlertDisposal alertDisposal, boolean add);

    QueryWrapper<AlertDisposal> getQueryWrapper(AlertDisposalQueryRequest alertDisposalQueryRequest);

    AlertDisposalVO getAlertDisposalVO(AlertDisposal alertDisposal);

    List<AlertDisposalVO> getAlertDisposalVO(List<AlertDisposal> alertDisposalList);
}
