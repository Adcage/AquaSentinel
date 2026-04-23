package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.springboot.common.ErrorCode;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.VenueMapper;
import com.springboot.model.dto.venue.VenueQueryRequest;
import com.springboot.model.entity.Venue;
import com.springboot.model.vo.VenueVO;
import com.springboot.service.VenueService;
import com.springboot.utils.SqlUtils;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【venue(场馆表)】的数据库操作Service实现
 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {

    @Override
    public void validVenue(Venue venue, boolean add) {
        if (venue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆信息不能为空");
        }
        if (add && StringUtils.isBlank(venue.getVenue_code())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆编码不能为空");
        }
        if (add && StringUtils.isBlank(venue.getVenue_name())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆名称不能为空");
        }
        if (StringUtils.isNotBlank(venue.getVenue_code()) && venue.getVenue_code().length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆编码过长");
        }
        if (StringUtils.isNotBlank(venue.getVenue_name()) && venue.getVenue_name().length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆名称过长");
        }
        if (venue.getStatus() != null
                && !Objects.equals(venue.getStatus(), 0)
                && !Objects.equals(venue.getStatus(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效");
        }
        if (StringUtils.isNotBlank(venue.getVenue_code())) {
            QueryWrapper<Venue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("venue_code", venue.getVenue_code());
            queryWrapper.eq("is_delete", 0);
            queryWrapper.ne(venue.getId() != null, "id", venue.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "场馆编码已存在");
            }
        }
    }

    @Override
    public QueryWrapper<Venue> getQueryWrapper(VenueQueryRequest venueQueryRequest) {
        if (venueQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<Venue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(venueQueryRequest.getId() != null, "id", venueQueryRequest.getId());
        queryWrapper.eq(
                StringUtils.isNotBlank(venueQueryRequest.getVenueCode()),
                "venue_code",
                venueQueryRequest.getVenueCode());
        queryWrapper.like(
                StringUtils.isNotBlank(venueQueryRequest.getVenueName()),
                "venue_name",
                venueQueryRequest.getVenueName());
        queryWrapper.like(
                StringUtils.isNotBlank(venueQueryRequest.getContactName()),
                "contact_name",
                venueQueryRequest.getContactName());
        queryWrapper.eq(
                venueQueryRequest.getStatus() != null, "status", venueQueryRequest.getStatus());
        queryWrapper.eq("is_delete", 0);
        String sortField = venueQueryRequest.getSortField();
        String sortOrder = venueQueryRequest.getSortOrder();
        queryWrapper.orderBy(
                SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
        return queryWrapper;
    }

    @Override
    public VenueVO getVenueVO(Venue venue) {
        if (venue == null) {
            return null;
        }
        VenueVO venueVO = new VenueVO();
        venueVO.setId(venue.getId());
        venueVO.setVenueCode(venue.getVenue_code());
        venueVO.setVenueName(venue.getVenue_name());
        venueVO.setAddress(venue.getAddress());
        venueVO.setContactName(venue.getContact_name());
        venueVO.setContactPhone(venue.getContact_phone());
        venueVO.setTimezone(venue.getTimezone());
        venueVO.setStatus(venue.getStatus());
        venueVO.setFenceGeoJson(venue.getFence_geo_json());
        venueVO.setCreatedAt(venue.getCreated_at());
        venueVO.setUpdatedAt(venue.getUpdated_at());
        return venueVO;
    }

    @Override
    public List<VenueVO> getVenueVO(List<Venue> venueList) {
        if (CollUtil.isEmpty(venueList)) {
            return new ArrayList<>();
        }
        return venueList.stream().map(this::getVenueVO).collect(Collectors.toList());
    }
}
