package com.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.venuezone.VenueZoneAddRequest;
import com.springboot.model.dto.venuezone.VenueZoneEditRequest;
import com.springboot.model.dto.venuezone.VenueZoneQueryRequest;
import com.springboot.model.dto.venuezone.VenueZoneUpdateRequest;
import com.springboot.model.entity.VenueZone;
import com.springboot.model.vo.VenueZoneVO;
import com.springboot.service.VenueZoneService;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/venue-zones")
public class VenueZoneController {

    @Resource
    private VenueZoneService venueZoneService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addVenueZone(@RequestBody VenueZoneAddRequest venueZoneAddRequest) {
        if (venueZoneAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        VenueZone venueZone = toVenueZone(venueZoneAddRequest);
        venueZone.setZone_type(StringUtils.defaultIfBlank(venueZone.getZone_type(), "POOL"));
        venueZone.setRisk_level(StringUtils.defaultIfBlank(venueZone.getRisk_level(), "LOW"));
        venueZone.setIs_delete(0);
        venueZoneService.validVenueZone(venueZone, true);
        boolean result = venueZoneService.save(venueZone);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(venueZone.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteVenueZone(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = venueZoneService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateVenueZone(@RequestBody VenueZoneUpdateRequest venueZoneUpdateRequest) {
        if (venueZoneUpdateRequest == null || venueZoneUpdateRequest.getId() == null || venueZoneUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        VenueZone venueZone = toVenueZone(venueZoneUpdateRequest);
        venueZoneService.validVenueZone(venueZone, false);
        boolean result = venueZoneService.updateById(venueZone);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editVenueZone(@RequestBody VenueZoneEditRequest venueZoneEditRequest) {
        if (venueZoneEditRequest == null || venueZoneEditRequest.getId() == null || venueZoneEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        VenueZone venueZone = new VenueZone();
        venueZone.setId(venueZoneEditRequest.getId());
        venueZone.setZone_name(venueZoneEditRequest.getZoneName());
        venueZone.setZone_type(venueZoneEditRequest.getZoneType());
        venueZone.setGeo_json(venueZoneEditRequest.getGeoJson());
        venueZone.setRisk_level(venueZoneEditRequest.getRiskLevel());
        venueZoneService.validVenueZone(venueZone, false);
        boolean result = venueZoneService.updateById(venueZone);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<VenueZone> getVenueZoneById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        VenueZone venueZone = venueZoneService.getById(id);
        ThrowUtils.throwIf(venueZone == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(venueZone);
    }

    @GetMapping("/get/vo")
    public BaseResponse<VenueZoneVO> getVenueZoneVOById(long id) {
        BaseResponse<VenueZone> response = getVenueZoneById(id);
        return ResultUtils.success(venueZoneService.getVenueZoneVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<VenueZone>> listVenueZoneByPage(@RequestBody VenueZoneQueryRequest venueZoneQueryRequest) {
        if (venueZoneQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = venueZoneQueryRequest.getCurrent();
        long size = venueZoneQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<VenueZone> venueZonePage = venueZoneService.page(new Page<>(current, size),
                venueZoneService.getQueryWrapper(venueZoneQueryRequest));
        return ResultUtils.success(venueZonePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<VenueZoneVO>> listVenueZoneVOByPage(@RequestBody VenueZoneQueryRequest venueZoneQueryRequest) {
        BaseResponse<Page<VenueZone>> response = listVenueZoneByPage(venueZoneQueryRequest);
        Page<VenueZone> venueZonePage = response.getData();
        Page<VenueZoneVO> venueZoneVOPage = new Page<>(venueZonePage.getCurrent(), venueZonePage.getSize(),
                venueZonePage.getTotal());
        List<VenueZoneVO> venueZoneVOList = venueZoneService.getVenueZoneVO(venueZonePage.getRecords());
        venueZoneVOPage.setRecords(venueZoneVOList);
        return ResultUtils.success(venueZoneVOPage);
    }

    private VenueZone toVenueZone(VenueZoneAddRequest request) {
        VenueZone venueZone = new VenueZone();
        venueZone.setVenue_id(request.getVenueId());
        venueZone.setZone_code(request.getZoneCode());
        venueZone.setZone_name(request.getZoneName());
        venueZone.setZone_type(request.getZoneType());
        venueZone.setGeo_json(request.getGeoJson());
        venueZone.setRisk_level(request.getRiskLevel());
        return venueZone;
    }

    private VenueZone toVenueZone(VenueZoneUpdateRequest request) {
        VenueZone venueZone = new VenueZone();
        venueZone.setId(request.getId());
        venueZone.setVenue_id(request.getVenueId());
        venueZone.setZone_code(request.getZoneCode());
        venueZone.setZone_name(request.getZoneName());
        venueZone.setZone_type(request.getZoneType());
        venueZone.setGeo_json(request.getGeoJson());
        venueZone.setRisk_level(request.getRiskLevel());
        return venueZone;
    }
}
