package com.springboot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.venuezone.VenueZoneQueryRequest;
import com.springboot.model.entity.VenueZone;
import com.springboot.model.vo.VenueZoneVO;
import com.springboot.service.VenueZoneService;
import com.springboot.mapper.VenueZoneMapper;
import com.springboot.utils.SqlUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @description 针对表【venue_zone(场馆区域表)】的数据库操作Service实现
*/
@Service
public class VenueZoneServiceImpl extends ServiceImpl<VenueZoneMapper, VenueZone>
    implements VenueZoneService{

    @Override
    public void validVenueZone(VenueZone venueZone, boolean add) {
        if (venueZone == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆区域信息不能为空");
        }
        if (add && venueZone.getVenue_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆ID不能为空");
        }
        if (add && StringUtils.isBlank(venueZone.getZone_code())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "区域编码不能为空");
        }
        if (add && StringUtils.isBlank(venueZone.getZone_name())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "区域名称不能为空");
        }
        if (StringUtils.isNotBlank(venueZone.getZone_code()) && venueZone.getZone_code().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "区域编码过长");
        }
        if (StringUtils.isNotBlank(venueZone.getZone_name()) && venueZone.getZone_name().length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "区域名称过长");
        }
        if (StringUtils.isNotBlank(venueZone.getZone_code())) {
            QueryWrapper<VenueZone> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("zone_code", venueZone.getZone_code());
            queryWrapper.eq("is_delete", 0);
            queryWrapper.ne(venueZone.getId() != null, "id", venueZone.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "区域编码已存在");
            }
        }
    }

    @Override
    public QueryWrapper<VenueZone> getQueryWrapper(VenueZoneQueryRequest venueZoneQueryRequest) {
        if (venueZoneQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<VenueZone> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(venueZoneQueryRequest.getId() != null, "id", venueZoneQueryRequest.getId());
        queryWrapper.eq(venueZoneQueryRequest.getVenueId() != null, "venue_id", venueZoneQueryRequest.getVenueId());
        queryWrapper.eq(StringUtils.isNotBlank(venueZoneQueryRequest.getZoneCode()), "zone_code",
                venueZoneQueryRequest.getZoneCode());
        queryWrapper.like(StringUtils.isNotBlank(venueZoneQueryRequest.getZoneName()), "zone_name",
                venueZoneQueryRequest.getZoneName());
        queryWrapper.eq(StringUtils.isNotBlank(venueZoneQueryRequest.getZoneType()), "zone_type",
                venueZoneQueryRequest.getZoneType());
        queryWrapper.eq(StringUtils.isNotBlank(venueZoneQueryRequest.getRiskLevel()), "risk_level",
                venueZoneQueryRequest.getRiskLevel());
        queryWrapper.eq("is_delete", 0);
        String sortField = venueZoneQueryRequest.getSortField();
        String sortOrder = venueZoneQueryRequest.getSortOrder();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public VenueZoneVO getVenueZoneVO(VenueZone venueZone) {
        if (venueZone == null) {
            return null;
        }
        VenueZoneVO venueZoneVO = new VenueZoneVO();
        venueZoneVO.setId(venueZone.getId());
        venueZoneVO.setVenueId(venueZone.getVenue_id());
        venueZoneVO.setZoneCode(venueZone.getZone_code());
        venueZoneVO.setZoneName(venueZone.getZone_name());
        venueZoneVO.setZoneType(venueZone.getZone_type());
        venueZoneVO.setGeoJson(venueZone.getGeo_json());
        venueZoneVO.setRiskLevel(venueZone.getRisk_level());
        venueZoneVO.setCreatedAt(venueZone.getCreated_at());
        venueZoneVO.setUpdatedAt(venueZone.getUpdated_at());
        return venueZoneVO;
    }

    @Override
    public List<VenueZoneVO> getVenueZoneVO(List<VenueZone> venueZoneList) {
        if (CollUtil.isEmpty(venueZoneList)) {
            return new ArrayList<>();
        }
        return venueZoneList.stream().map(this::getVenueZoneVO).collect(Collectors.toList());
    }
}




