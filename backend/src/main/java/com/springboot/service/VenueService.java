package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.Venue;
import com.springboot.model.dto.venue.VenueQueryRequest;
import com.springboot.model.vo.VenueVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【venue(场馆表)】的数据库操作Service
*/
public interface VenueService extends IService<Venue> {

    void validVenue(Venue venue, boolean add);

    QueryWrapper<Venue> getQueryWrapper(VenueQueryRequest venueQueryRequest);

    VenueVO getVenueVO(Venue venue);

    List<VenueVO> getVenueVO(List<Venue> venueList);
}
