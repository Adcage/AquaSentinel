package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.auth.LoginRequest;
import com.springboot.model.dto.lifeguard.LifeguardAddRequest;
import com.springboot.model.dto.lifeguard.LifeguardAuditRequest;
import com.springboot.model.dto.lifeguard.LifeguardDutyUpdateRequest;
import com.springboot.model.dto.lifeguard.LifeguardEditRequest;
import com.springboot.model.dto.lifeguard.LifeguardOffPostCheckRequest;
import com.springboot.model.dto.lifeguard.LifeguardLeaveReportRequest;
import com.springboot.model.dto.lifeguard.LifeguardQueryRequest;
import com.springboot.model.dto.lifeguard.LifeguardUpdateRequest;
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationReportRequest;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.entity.Venue;
import com.springboot.model.entity.SysRole;
import com.springboot.model.entity.SysUser;
import com.springboot.model.entity.SysUserRole;
import com.springboot.model.vo.LifeguardLocationLogVO;
import com.springboot.model.vo.LoginResultVO;
import com.springboot.model.vo.LifeguardVO;
import com.springboot.service.AuthService;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.service.LifeguardService;
import com.springboot.service.VenueService;
import com.springboot.service.impl.LifeguardOffPostAlertService;
import com.springboot.mapper.SysRoleMapper;
import com.springboot.mapper.SysUserMapper;
import com.springboot.mapper.SysUserRoleMapper;
import com.springboot.utils.PasswordHashUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.math.BigDecimal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/lifeguards")
public class LifeguardController {

    @Resource
    private LifeguardService lifeguardService;

    @Resource
    private LifeguardLocationLogService lifeguardLocationLogService;

    @Resource
    private LifeguardOffPostAlertService lifeguardOffPostAlertService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AuthService authService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private VenueService venueService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Long> addLifeguard(@RequestBody LifeguardAddRequest lifeguardAddRequest) {
        if (lifeguardAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Lifeguard lifeguard = toLifeguard(lifeguardAddRequest);
        lifeguard.setUser_id(resolveOrCreateLifeguardUserId(lifeguardAddRequest));
        lifeguard.setAudit_status(lifeguard.getAudit_status() == null ? "PENDING" : lifeguard.getAudit_status());
        lifeguard.setDuty_status(lifeguard.getDuty_status() == null ? "OFF_DUTY" : lifeguard.getDuty_status());
        if (StringUtils.isBlank(lifeguard.getLifeguard_code())) {
            lifeguard.setLifeguard_code("LG-" + System.currentTimeMillis());
        }
        if (lifeguard.getFence_geo_json() == null) {
            lifeguard.setFence_geo_json("{}");
        }
        lifeguard.setIs_delete(0);
        lifeguardService.validLifeguard(lifeguard, true);
        boolean result = lifeguardService.save(lifeguard);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(lifeguard.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteLifeguard(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = lifeguardService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateLifeguard(@RequestBody LifeguardUpdateRequest lifeguardUpdateRequest) {
        if (lifeguardUpdateRequest == null || lifeguardUpdateRequest.getId() == null || lifeguardUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Lifeguard lifeguard = toLifeguard(lifeguardUpdateRequest);
        lifeguardService.validLifeguard(lifeguard, false);
        boolean result = lifeguardService.updateById(lifeguard);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editLifeguard(@RequestBody LifeguardEditRequest lifeguardEditRequest) {
        if (lifeguardEditRequest == null || lifeguardEditRequest.getId() == null || lifeguardEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Lifeguard lifeguard = new Lifeguard();
        lifeguard.setId(lifeguardEditRequest.getId());
        lifeguard.setFull_name(lifeguardEditRequest.getFullName());
        lifeguard.setPhone(lifeguardEditRequest.getPhone());
        lifeguard.setVenue_id(lifeguardEditRequest.getVenueId());
        lifeguard.setFence_geo_json(lifeguardEditRequest.getFenceGeoJson());
        lifeguard.setAudit_status(lifeguardEditRequest.getAuditStatus());
        lifeguard.setDuty_status(lifeguardEditRequest.getDutyStatus());
        lifeguardService.validLifeguard(lifeguard, false);
        boolean result = lifeguardService.updateById(lifeguard);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Lifeguard> getLifeguardById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("is_delete", 0);
        Lifeguard lifeguard = lifeguardService.getOne(queryWrapper);
        ThrowUtils.throwIf(lifeguard == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(lifeguard);
    }

    @GetMapping("/get/vo")
    public BaseResponse<LifeguardVO> getLifeguardVOById(long id) {
        BaseResponse<Lifeguard> response = getLifeguardById(id);
        return ResultUtils.success(lifeguardService.getLifeguardVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<Lifeguard>> listLifeguardByPage(@RequestBody LifeguardQueryRequest lifeguardQueryRequest) {
        if (lifeguardQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = lifeguardQueryRequest.getCurrent();
        long size = lifeguardQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<Lifeguard> lifeguardPage = lifeguardService.page(new Page<>(current, size),
                lifeguardService.getQueryWrapper(lifeguardQueryRequest));
        return ResultUtils.success(lifeguardPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<LifeguardVO>> listLifeguardVOByPage(@RequestBody LifeguardQueryRequest lifeguardQueryRequest) {
        BaseResponse<Page<Lifeguard>> response = listLifeguardByPage(lifeguardQueryRequest);
        Page<Lifeguard> lifeguardPage = response.getData();
        Page<LifeguardVO> lifeguardVOPage = new Page<>(lifeguardPage.getCurrent(), lifeguardPage.getSize(),
                lifeguardPage.getTotal());
        List<LifeguardVO> lifeguardVOList = lifeguardService.getLifeguardVO(lifeguardPage.getRecords());
        lifeguardVOPage.setRecords(lifeguardVOList);
        return ResultUtils.success(lifeguardVOPage);
    }

    @PostMapping("/audit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> auditLifeguard(@RequestBody LifeguardAuditRequest lifeguardAuditRequest) {
        if (lifeguardAuditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = lifeguardService.audit(lifeguardAuditRequest.getLifeguardId(),
                lifeguardAuditRequest.getAuditStatus(), lifeguardAuditRequest.getApprovedBy());
        return ResultUtils.success(result);
    }

    @PostMapping("/{id}/audit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> auditLifeguardByPath(@PathVariable("id") Long id,
            @RequestBody LifeguardAuditRequest request) {
        LifeguardAuditRequest auditRequest = request == null ? new LifeguardAuditRequest() : request;
        auditRequest.setLifeguardId(id);
        return auditLifeguard(auditRequest);
    }

    @PostMapping("/duty/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN + "," + RoleConstant.LIFEGUARD)
    public BaseResponse<Boolean> updateDutyStatus(@RequestBody LifeguardDutyUpdateRequest lifeguardDutyUpdateRequest) {
        if (lifeguardDutyUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = lifeguardService.updateDutyStatus(lifeguardDutyUpdateRequest.getLifeguardId(),
                lifeguardDutyUpdateRequest.getDutyStatus(), lifeguardDutyUpdateRequest.getOperatorId());
        return ResultUtils.success(result);
    }

    @PostMapping("/{id}/duty")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN + "," + RoleConstant.LIFEGUARD)
    public BaseResponse<Boolean> updateDutyStatusByPath(@PathVariable("id") Long id,
            @RequestBody LifeguardDutyUpdateRequest request) {
        LifeguardDutyUpdateRequest dutyRequest = request == null ? new LifeguardDutyUpdateRequest() : request;
        dutyRequest.setLifeguardId(id);
        return updateDutyStatus(dutyRequest);
    }

    @PostMapping("/leave-report")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN + "," + RoleConstant.LIFEGUARD)
    public BaseResponse<Boolean> submitLeaveReport(@RequestBody LifeguardLeaveReportRequest lifeguardLeaveReportRequest) {
        if (lifeguardLeaveReportRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = lifeguardService.submitLeaveReport(lifeguardLeaveReportRequest.getLifeguardId(),
                lifeguardLeaveReportRequest.getLeaveReason(), lifeguardLeaveReportRequest.getPlannedReturnAt());
        return ResultUtils.success(result);
    }

    @PostMapping("/{id}/leave-report")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN + "," + RoleConstant.LIFEGUARD)
    public BaseResponse<Boolean> submitLeaveReportByPath(@PathVariable("id") Long id,
            @RequestBody LifeguardLeaveReportRequest request) {
        LifeguardLeaveReportRequest leaveRequest = request == null ? new LifeguardLeaveReportRequest() : request;
        leaveRequest.setLifeguardId(id);
        return submitLeaveReport(leaveRequest);
    }

    @PostMapping("/location/report")
    public BaseResponse<Map<String, Object>> reportLocation(
            @RequestBody LifeguardLocationReportRequest lifeguardLocationReportRequest) {
        ensureLocationPermission();
        if (lifeguardLocationReportRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Lifeguard lifeguard = lifeguardService.getById(lifeguardLocationReportRequest.getLifeguardId());
        if (lifeguard == null || Integer.valueOf(1).equals(lifeguard.getIs_delete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "救生员不存在");
        }
        Long venueId = lifeguard.getVenue_id();
        if (venueId == null || venueId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员未绑定场馆");
        }
        LifeguardLocationLog lifeguardLocationLog = new LifeguardLocationLog();
        lifeguardLocationLog.setLifeguard_id(lifeguardLocationReportRequest.getLifeguardId());
        lifeguardLocationLog.setVenue_id(venueId);
        lifeguardLocationLog.setLongitude(lifeguardLocationReportRequest.getLongitude());
        lifeguardLocationLog.setLatitude(lifeguardLocationReportRequest.getLatitude());
        lifeguardLocationLog.setIn_fence(resolveInFence(
                venueId,
                lifeguardLocationReportRequest.getLongitude(),
                lifeguardLocationReportRequest.getLatitude()));
        lifeguardLocationLog.setReport_source(lifeguardLocationReportRequest.getReportSource());
        lifeguardLocationLog.setReported_at(lifeguardLocationReportRequest.getReportedAt());
        boolean result = lifeguardLocationLogService.reportLocation(lifeguardLocationLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        Map<String, Object> response = new HashMap<>(
                lifeguardOffPostAlertService.checkAfterLocationReport(lifeguardLocationLog));
        response.put("saved", true);
        return ResultUtils.success(response);
    }

    @PostMapping("/{id}/locations")
    public BaseResponse<Map<String, Object>> reportLocationByPath(@PathVariable("id") Long id,
            @RequestBody LifeguardLocationReportRequest request) {
        LifeguardLocationReportRequest locationRequest = request == null ? new LifeguardLocationReportRequest() : request;
        locationRequest.setLifeguardId(id);
        return reportLocation(locationRequest);
    }

    @PostMapping("/login")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<LoginResultVO> lifeguardLogin(@RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        LoginResultVO loginResultVO = authService.login(request, httpServletRequest);
        if (loginResultVO.getUser() == null || loginResultVO.getUser().getId() == null
                || loginResultVO.getUser().getRoles() == null
                || !loginResultVO.getUser().getRoles().contains(RoleConstant.LIFEGUARD)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前账号不是救生员账号");
        }
        QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginResultVO.getUser().getId());
        queryWrapper.eq("is_delete", 0);
        Lifeguard lifeguard = lifeguardService.getOne(queryWrapper);
        if (lifeguard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到救生员档案");
        }
        if (!"APPROVED".equalsIgnoreCase(lifeguard.getAudit_status())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "救生员账号未审核通过");
        }
        lifeguardService.updateDutyStatus(lifeguard.getId(), "ON_DUTY", loginResultVO.getUser().getId());
        return ResultUtils.success(loginResultVO);
    }

    @PostMapping("/offpost/check")
    public BaseResponse<Map<String, Object>> offPostCheck(@RequestBody LifeguardOffPostCheckRequest request) {
        ensureLocationPermission();
        if (request == null || request.getLifeguardId() == null || request.getLifeguardId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "lifeguardId不能为空");
        }
        Map<String, Object> data = new HashMap<>(lifeguardOffPostAlertService.checkByLifeguard(request.getLifeguardId()));
        if (request.getLeaveReason() != null) {
            data.put("leaveReason", request.getLeaveReason());
        }
        if (request.getPlannedReturnAt() != null) {
            data.put("plannedReturnAt", request.getPlannedReturnAt());
        }
        return ResultUtils.success(data);
    }

    @GetMapping("/location/recent")
    public BaseResponse<List<LifeguardLocationLogVO>> recentLocations(@RequestParam("lifeguardId") Long lifeguardId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<LifeguardLocationLog> lifeguardLocationLogs = lifeguardLocationLogService.recentLocations(lifeguardId, limit);
        return ResultUtils.success(lifeguardLocationLogService.getLifeguardLocationLogVO(lifeguardLocationLogs));
    }

    private Lifeguard toLifeguard(LifeguardAddRequest request) {
        Lifeguard lifeguard = new Lifeguard();
        lifeguard.setUser_id(request.getUserId());
        lifeguard.setLifeguard_code(request.getLifeguardCode());
        lifeguard.setFull_name(request.getFullName());
        lifeguard.setPhone(request.getPhone());
        lifeguard.setVenue_id(request.getVenueId());
        lifeguard.setFence_geo_json(toJsonText(request.getFenceGeoJson()));
        lifeguard.setAudit_status(request.getAuditStatus());
        lifeguard.setDuty_status(request.getDutyStatus());
        return lifeguard;
    }

    private Long resolveOrCreateLifeguardUserId(LifeguardAddRequest request) {
        if (request.getUserId() != null && request.getUserId() > 0) {
            SysUser sysUser = sysUserMapper.selectById(request.getUserId());
            if (sysUser == null || Integer.valueOf(1).equals(sysUser.getIs_delete())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "userId对应账号不存在");
            }
            ensureUserNotBoundByOtherLifeguard(request.getUserId(), null);
            ensureLifeguardRoleGranted(request.getUserId());
            return request.getUserId();
        }
        if (StringUtils.isAnyBlank(request.getUsername(), request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建救生员需提供userId或username/password");
        }
        if (request.getPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员账号密码长度不能少于6位");
        }
        QueryWrapper<SysUser> userQuery = new QueryWrapper<>();
        userQuery.eq("username", request.getUsername().trim());
        userQuery.eq("is_delete", 0);
        if (sysUserMapper.selectCount(userQuery) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "救生员账号已存在");
        }
        if (StringUtils.isNotBlank(request.getPhone())) {
            QueryWrapper<SysUser> phoneQuery = new QueryWrapper<>();
            phoneQuery.eq("phone", request.getPhone().trim());
            phoneQuery.eq("is_delete", 0);
            if (sysUserMapper.selectCount(phoneQuery) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号已存在");
            }
        }

        SysUser sysUser = new SysUser();
        sysUser.setUsername(request.getUsername().trim());
        sysUser.setPassword_hash(PasswordHashUtils.md5WithSalt(request.getPassword()));
        sysUser.setDisplay_name(StringUtils.defaultIfBlank(request.getFullName(), request.getUsername().trim()));
        sysUser.setPhone(request.getPhone());
        sysUser.setEmail(request.getEmail());
        sysUser.setStatus(1);
        sysUser.setFailed_login_count(0);
        sysUser.setForce_change_password(1);
        sysUser.setCreated_at(new Date());
        sysUser.setUpdated_at(new Date());
        sysUser.setIs_delete(0);
        int inserted = sysUserMapper.insert(sysUser);
        if (inserted != 1 || sysUser.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "救生员账号创建失败");
        }

        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("role_code", RoleConstant.LIFEGUARD);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        SysRole role = sysRoleMapper.selectOne(roleQuery);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统未配置LIFEGUARD角色");
        }
        SysUserRole sysUserRole = new SysUserRole();
        sysUserRole.setUser_id(sysUser.getId());
        sysUserRole.setRole_id(role.getId());
        sysUserRole.setCreated_at(new Date());
        sysUserRoleMapper.insert(sysUserRole);
        return sysUser.getId();
    }

    private void ensureUserNotBoundByOtherLifeguard(Long userId, Long ignoreLifeguardId) {
        QueryWrapper<Lifeguard> lifeguardQuery = new QueryWrapper<>();
        lifeguardQuery.eq("user_id", userId);
        lifeguardQuery.eq("is_delete", 0);
        lifeguardQuery.ne(ignoreLifeguardId != null && ignoreLifeguardId > 0, "id", ignoreLifeguardId);
        Long count = lifeguardService.count(lifeguardQuery);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已绑定其他救生员");
        }
    }

    private void ensureLifeguardRoleGranted(Long userId) {
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("role_code", RoleConstant.LIFEGUARD);
        roleQuery.eq("status", 1);
        roleQuery.eq("is_delete", 0);
        SysRole role = sysRoleMapper.selectOne(roleQuery);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统未配置LIFEGUARD角色");
        }

        QueryWrapper<SysUserRole> relationQuery = new QueryWrapper<>();
        relationQuery.eq("user_id", userId);
        relationQuery.eq("role_id", role.getId());
        if (sysUserRoleMapper.selectCount(relationQuery) > 0) {
            return;
        }
        SysUserRole relation = new SysUserRole();
        relation.setUser_id(userId);
        relation.setRole_id(role.getId());
        relation.setCreated_at(new Date());
        sysUserRoleMapper.insert(relation);
    }

    private Lifeguard toLifeguard(LifeguardUpdateRequest request) {
        Lifeguard lifeguard = new Lifeguard();
        lifeguard.setId(request.getId());
        lifeguard.setUser_id(request.getUserId());
        lifeguard.setLifeguard_code(request.getLifeguardCode());
        lifeguard.setFull_name(request.getFullName());
        lifeguard.setPhone(request.getPhone());
        lifeguard.setVenue_id(request.getVenueId());
        lifeguard.setFence_geo_json(toJsonText(request.getFenceGeoJson()));
        lifeguard.setAudit_status(request.getAuditStatus());
        lifeguard.setDuty_status(request.getDutyStatus());
        return lifeguard;
    }

    private void ensureLocationPermission() {
        AuthUserContext authUserContext = AuthContextHolder.getRequired();
        boolean hasPermission = authUserContext.hasRole(RoleConstant.SUPER_ADMIN)
                || authUserContext.hasRole(RoleConstant.VENUE_ADMIN)
                || authUserContext.hasRole(RoleConstant.LIFEGUARD);
        if (!hasPermission) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前角色无定位/脱岗操作权限");
        }
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

    private Integer resolveInFence(Long venueId, BigDecimal longitude, BigDecimal latitude) {
        int fallback = 1;
        if (venueId == null || venueId <= 0 || longitude == null || latitude == null) {
            return fallback;
        }
        Venue venue = venueService.getById(venueId);
        if (venue == null) {
            return fallback;
        }
        JsonNode fenceNode = parseFenceNode(venue.getFence_geo_json());
        if (fenceNode == null) {
            return fallback;
        }
        List<List<double[]>> polygons = extractPolygons(fenceNode);
        if (polygons.isEmpty()) {
            return fallback;
        }
        boolean inFence = polygons.stream()
                .anyMatch(polygon -> isPointInPolygon(longitude.doubleValue(), latitude.doubleValue(), polygon));
        return inFence ? 1 : 0;
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

    private List<List<double[]>> extractPolygons(JsonNode fenceNode) {
        List<List<double[]>> polygons = new java.util.ArrayList<>();
        String rootType = fenceNode.path("type").asText("");
        if ("FeatureCollection".equalsIgnoreCase(rootType)) {
            JsonNode features = fenceNode.path("features");
            if (features.isArray()) {
                for (JsonNode featureNode : features) {
                    collectPolygons(featureNode.path("geometry"), polygons);
                }
            }
            return polygons;
        } else if ("Feature".equalsIgnoreCase(rootType)) {
            collectPolygons(fenceNode.path("geometry"), polygons);
            return polygons;
        }
        collectPolygons(fenceNode, polygons);
        return polygons;
    }

    private void collectPolygons(JsonNode geometryNode, List<List<double[]>> polygons) {
        String geometryType = geometryNode.path("type").asText("");
        JsonNode coordinates = geometryNode.path("coordinates");
        if (!coordinates.isArray() || coordinates.isEmpty()) {
            return;
        }
        if ("Polygon".equalsIgnoreCase(geometryType)) {
            List<double[]> ring = toCoordinateList(coordinates.get(0));
            if (ring.size() >= 3) {
                polygons.add(ring);
            }
            return;
        }
        if ("MultiPolygon".equalsIgnoreCase(geometryType)) {
            for (JsonNode polygonNode : coordinates) {
                if (!polygonNode.isArray() || polygonNode.isEmpty()) {
                    continue;
                }
                List<double[]> ring = toCoordinateList(polygonNode.get(0));
                if (ring.size() >= 3) {
                    polygons.add(ring);
                }
            }
        }
    }

    private List<double[]> toCoordinateList(JsonNode ringNode) {
        if (ringNode == null || !ringNode.isArray()) {
            return List.of();
        }
        List<double[]> points = new java.util.ArrayList<>();
        for (JsonNode point : ringNode) {
            if (!point.isArray() || point.size() < 2) {
                continue;
            }
            if (!point.get(0).isNumber() || !point.get(1).isNumber()) {
                continue;
            }
            points.add(new double[] { point.get(0).asDouble(), point.get(1).asDouble() });
        }
        return points;
    }

    private boolean isPointInPolygon(double x, double y, List<double[]> polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i)[0];
            double yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0];
            double yj = polygon.get(j)[1];
            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / ((yj - yi) + 1e-12) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }
}
