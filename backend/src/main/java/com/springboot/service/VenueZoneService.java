package com.springboot.service;

import java.util.List;

import com.springboot.model.dto.venuezone.VenueZoneQueryRequest;
import com.springboot.model.entity.VenueZone;
import com.springboot.model.vo.VenueZoneVO;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【venue_zone(场馆区域表)】的数据库操作Service
 */
public interface VenueZoneService extends IService<VenueZone> {

    void validVenueZone(VenueZone venueZone, boolean add);

    QueryWrapper<VenueZone> getQueryWrapper(VenueZoneQueryRequest venueZoneQueryRequest);

    VenueZoneVO getVenueZoneVO(VenueZone venueZone);

    List<VenueZoneVO> getVenueZoneVO(List<VenueZone> venueZoneList);
}
