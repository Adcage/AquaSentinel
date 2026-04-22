package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.venue.VenueAddRequest;
import com.springboot.model.dto.venue.VenueEditRequest;
import com.springboot.model.dto.venue.VenueFenceBoundsRequest;
import com.springboot.model.dto.venue.VenueQueryRequest;
import com.springboot.model.dto.venue.VenueUpdateRequest;
import com.springboot.model.entity.Venue;
import com.springboot.model.vo.VenueVO;
import com.springboot.service.VenueService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/venues")
public class VenueController {

    @Resource
    private VenueService venueService;

    @Resource
    private ObjectMapper objectMapper;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addVenue(@RequestBody VenueAddRequest venueAddRequest) {
        if (venueAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Venue venue = toVenue(venueAddRequest);
        venue.setContact_name(StringUtils.defaultIfBlank(venue.getContact_name(), "未设置"));
        venue.setContact_phone(StringUtils.defaultIfBlank(venue.getContact_phone(), "未设置"));
        venue.setTimezone(StringUtils.defaultIfBlank(venue.getTimezone(), "Asia/Shanghai"));
        venue.setStatus(venue.getStatus() == null ? 1 : venue.getStatus());
        venue.setIs_delete(0);
        venueService.validVenue(venue, true);
        boolean result;
        try {
            result = venueService.save(venue);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "场馆信息不完整，请补充必填字段");
        }
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(venue.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteVenue(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result;
        try {
            result = venueService.removeById(deleteRequest.getId());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "场馆已被报警记录或其他业务数据引用，无法删除");
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateVenue(@RequestBody VenueUpdateRequest venueUpdateRequest) {
        if (venueUpdateRequest == null || venueUpdateRequest.getId() == null || venueUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Venue venue = toVenue(venueUpdateRequest);
        venueService.validVenue(venue, false);
        boolean result = venueService.updateById(venue);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editVenue(@RequestBody VenueEditRequest venueEditRequest) {
        if (venueEditRequest == null || venueEditRequest.getId() == null || venueEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Venue venue = new Venue();
        venue.setId(venueEditRequest.getId());
        venue.setVenue_name(venueEditRequest.getVenueName());
        venue.setAddress(venueEditRequest.getAddress());
        venue.setContact_name(venueEditRequest.getContactName());
        venue.setContact_phone(venueEditRequest.getContactPhone());
        venue.setTimezone(venueEditRequest.getTimezone());
        venue.setStatus(venueEditRequest.getStatus());
        venue.setFence_geo_json(toJsonText(venueEditRequest.getFenceGeoJson()));
        venueService.validVenue(venue, false);
        boolean result = venueService.updateById(venue);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Venue> getVenueById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Venue venue = venueService.getById(id);
        ThrowUtils.throwIf(venue == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(venue);
    }

    @GetMapping("/get/vo")
    public BaseResponse<VenueVO> getVenueVOById(long id) {
        BaseResponse<Venue> response = getVenueById(id);
        return ResultUtils.success(venueService.getVenueVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<Venue>> listVenueByPage(@RequestBody VenueQueryRequest venueQueryRequest) {
        if (venueQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = venueQueryRequest.getCurrent();
        long size = venueQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<Venue> venuePage = venueService.page(new Page<>(current, size), venueService.getQueryWrapper(venueQueryRequest));
        return ResultUtils.success(venuePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<VenueVO>> listVenueVOByPage(@RequestBody VenueQueryRequest venueQueryRequest) {
        BaseResponse<Page<Venue>> response = listVenueByPage(venueQueryRequest);
        Page<Venue> venuePage = response.getData();
        Page<VenueVO> venueVOPage = new Page<>(venuePage.getCurrent(), venuePage.getSize(), venuePage.getTotal());
        List<VenueVO> venueVOList = venueService.getVenueVO(venuePage.getRecords());
        venueVOPage.setRecords(venueVOList);
        return ResultUtils.success(venueVOPage);
    }

    @PostMapping("/list/fence/bounds")
    public BaseResponse<Page<VenueVO>> listVenueFenceByBounds(@RequestBody VenueFenceBoundsRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (request.getMinLng() == null || request.getMaxLng() == null
                || request.getMinLat() == null || request.getMaxLat() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "视野范围参数不能为空");
        }
        if (request.getMinLng() >= request.getMaxLng() || request.getMinLat() >= request.getMaxLat()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "视野范围参数无效");
        }
        long current = request.getCurrent() > 0 ? request.getCurrent() : 1;
        long size = request.getPageSize() > 0 ? request.getPageSize() : 20;
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");

        QueryWrapper<Venue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq(request.getStatus() != null, "status", request.getStatus());
        queryWrapper.isNotNull("fence_geo_json");
        List<Venue> venueList = venueService.list(queryWrapper);

        List<VenueVO> matchedVenueList = new ArrayList<>();
        for (Venue venue : venueList) {
            if (!isFenceIntersectsBounds(venue.getFence_geo_json(), request)) {
                continue;
            }
            matchedVenueList.add(venueService.getVenueVO(venue));
        }

        int total = matchedVenueList.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = Math.min(fromIndex + (int) size, total);
        List<VenueVO> records = fromIndex >= total
                ? List.of()
                : matchedVenueList.subList(fromIndex, toIndex);
        Page<VenueVO> venueVOPage = new Page<>(current, size, total);
        venueVOPage.setRecords(records);
        return ResultUtils.success(venueVOPage);
    }

    private Venue toVenue(VenueAddRequest request) {
        Venue venue = new Venue();
        venue.setVenue_code(request.getVenueCode());
        venue.setVenue_name(request.getVenueName());
        venue.setAddress(request.getAddress());
        venue.setContact_name(request.getContactName());
        venue.setContact_phone(request.getContactPhone());
        venue.setTimezone(request.getTimezone());
        venue.setStatus(request.getStatus());
        venue.setFence_geo_json(toJsonText(request.getFenceGeoJson()));
        return venue;
    }

    private Venue toVenue(VenueUpdateRequest request) {
        Venue venue = new Venue();
        venue.setId(request.getId());
        venue.setVenue_code(request.getVenueCode());
        venue.setVenue_name(request.getVenueName());
        venue.setAddress(request.getAddress());
        venue.setContact_name(request.getContactName());
        venue.setContact_phone(request.getContactPhone());
        venue.setTimezone(request.getTimezone());
        venue.setStatus(request.getStatus());
        venue.setFence_geo_json(toJsonText(request.getFenceGeoJson()));
        return venue;
    }

    private String toJsonText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private boolean isFenceIntersectsBounds(Object fenceValue, VenueFenceBoundsRequest request) {
        JsonNode fenceNode = parseFenceNode(fenceValue);
        if (fenceNode == null) {
            return false;
        }
        double[] bounds = new double[] {
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
        };
        mergeBoundsFromFence(fenceNode, bounds);
        if (!hasBounds(bounds)) {
            return false;
        }
        double minLng = request.getMinLng();
        double maxLng = request.getMaxLng();
        double minLat = request.getMinLat();
        double maxLat = request.getMaxLat();
        return !(bounds[1] < minLng || bounds[0] > maxLng || bounds[3] < minLat || bounds[2] > maxLat);
    }

    private JsonNode parseFenceNode(Object fenceValue) {
        if (fenceValue == null) {
            return null;
        }
        try {
            if (fenceValue instanceof String text) {
                if (StringUtils.isBlank(text)) {
                    return null;
                }
                return objectMapper.readTree(text);
            }
            return objectMapper.valueToTree(fenceValue);
        } catch (Exception e) {
            return null;
        }
    }

    private void mergeBoundsFromFence(JsonNode fenceNode, double[] bounds) {
        String rootType = fenceNode.path("type").asText("");
        if ("FeatureCollection".equalsIgnoreCase(rootType)) {
            JsonNode features = fenceNode.path("features");
            if (features.isArray()) {
                for (JsonNode featureNode : features) {
                    mergeBoundsFromGeometry(featureNode.path("geometry"), bounds);
                }
            }
            return;
        }
        if ("Feature".equalsIgnoreCase(rootType)) {
            mergeBoundsFromGeometry(fenceNode.path("geometry"), bounds);
            return;
        }
        mergeBoundsFromGeometry(fenceNode, bounds);
    }

    private void mergeBoundsFromGeometry(JsonNode geometryNode, double[] bounds) {
        String geometryType = geometryNode.path("type").asText("");
        JsonNode coordinates = geometryNode.path("coordinates");
        if (!coordinates.isArray() || coordinates.isEmpty()) {
            return;
        }
        if ("Polygon".equalsIgnoreCase(geometryType)) {
            mergeBoundsFromRing(coordinates.get(0), bounds);
            return;
        }
        if ("MultiPolygon".equalsIgnoreCase(geometryType)) {
            for (JsonNode polygonNode : coordinates) {
                if (!polygonNode.isArray() || polygonNode.isEmpty()) {
                    continue;
                }
                mergeBoundsFromRing(polygonNode.get(0), bounds);
            }
        }
    }

    private void mergeBoundsFromRing(JsonNode ringNode, double[] bounds) {
        if (ringNode == null || !ringNode.isArray()) {
            return;
        }
        for (JsonNode pointNode : ringNode) {
            if (!pointNode.isArray() || pointNode.size() < 2) {
                continue;
            }
            if (!pointNode.get(0).isNumber() || !pointNode.get(1).isNumber()) {
                continue;
            }
            double lng = pointNode.get(0).asDouble();
            double lat = pointNode.get(1).asDouble();
            bounds[0] = Math.min(bounds[0], lng);
            bounds[1] = Math.max(bounds[1], lng);
            bounds[2] = Math.min(bounds[2], lat);
            bounds[3] = Math.max(bounds[3], lat);
        }
    }

    private boolean hasBounds(double[] bounds) {
        return bounds[0] != Double.POSITIVE_INFINITY
                && bounds[1] != Double.NEGATIVE_INFINITY
                && bounds[2] != Double.POSITIVE_INFINITY
                && bounds[3] != Double.NEGATIVE_INFINITY;
    }
}
