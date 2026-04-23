package com.springboot.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.entity.VenueZone;
import com.springboot.service.AlertRecordService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.service.LifeguardService;
import com.springboot.service.VenueZoneService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class AlertDispatchRoutingService {

    private static final List<String> ACTIVE_ALERT_STATUSES =
            List.of("PENDING", "ASSIGNED", "CONFIRMED");

    @Resource private LifeguardService lifeguardService;

    @Resource private LifeguardLocationLogService lifeguardLocationLogService;

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private VenueZoneService venueZoneService;

    @Resource private AlertRecordService alertRecordService;

    @Resource private ObjectMapper objectMapper;

    public Lifeguard resolveAssignee(Long venueId, Long cameraId) {
        if (venueId == null || venueId <= 0) {
            return null;
        }
        QueryWrapper<Lifeguard> candidateQuery = new QueryWrapper<>();
        candidateQuery.eq("venue_id", venueId);
        candidateQuery.eq("audit_status", "APPROVED");
        candidateQuery.eq("duty_status", "ON_DUTY");
        candidateQuery.eq("is_delete", 0);
        List<Lifeguard> candidates = lifeguardService.list(candidateQuery);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<List<double[]>> zonePolygons = resolveZonePolygons(cameraId);
        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (Lifeguard lifeguard : candidates) {
            if (lifeguard == null || lifeguard.getId() == null) {
                continue;
            }
            LifeguardLocationLog latest = fetchLatestLocation(lifeguard.getId());
            boolean inFence = latest != null && Integer.valueOf(1).equals(latest.getIn_fence());
            boolean inZone = isInCameraZone(latest, zonePolygons);
            long activeAlertCount = countActiveAlerts(lifeguard.getId());
            long reportTime =
                    latest == null || latest.getReported_at() == null
                            ? 0L
                            : latest.getReported_at().getTime();
            scoredCandidates.add(
                    new ScoredCandidate(lifeguard, inZone, inFence, activeAlertCount, reportTime));
        }
        if (scoredCandidates.isEmpty()) {
            return null;
        }

        scoredCandidates.sort(
                Comparator.comparing(ScoredCandidate::inZone, Comparator.reverseOrder())
                        .thenComparing(ScoredCandidate::inFence, Comparator.reverseOrder())
                        .thenComparingLong(ScoredCandidate::activeAlertCount)
                        .thenComparing(
                                Comparator.comparingLong(ScoredCandidate::reportTimeMillis)
                                        .reversed())
                        .thenComparingLong(item -> item.lifeguard().getId()));
        return scoredCandidates.get(0).lifeguard();
    }

    private long countActiveAlerts(Long lifeguardId) {
        QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lifeguard_id", lifeguardId);
        queryWrapper.in("alert_status", ACTIVE_ALERT_STATUSES);
        Long count = alertRecordService.count(queryWrapper);
        return count == null ? 0L : count;
    }

    private LifeguardLocationLog fetchLatestLocation(Long lifeguardId) {
        List<LifeguardLocationLog> locations =
                lifeguardLocationLogService.recentLocations(lifeguardId, 1);
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        return locations.get(0);
    }

    private List<List<double[]>> resolveZonePolygons(Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            return List.of();
        }
        CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
        if (cameraDevice == null || cameraDevice.getZone_id() == null) {
            return List.of();
        }
        VenueZone zone = venueZoneService.getById(cameraDevice.getZone_id());
        if (zone == null) {
            return List.of();
        }
        JsonNode geoNode = parseGeoNode(zone.getGeo_json());
        if (geoNode == null) {
            return List.of();
        }
        return extractPolygons(geoNode);
    }

    private boolean isInCameraZone(
            LifeguardLocationLog latestLocation, List<List<double[]>> polygons) {
        if (latestLocation == null
                || latestLocation.getLongitude() == null
                || latestLocation.getLatitude() == null) {
            return false;
        }
        if (polygons == null || polygons.isEmpty()) {
            return false;
        }
        double longitude = latestLocation.getLongitude().doubleValue();
        double latitude = latestLocation.getLatitude().doubleValue();
        for (List<double[]> polygon : polygons) {
            if (isPointInPolygon(longitude, latitude, polygon)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode parseGeoNode(Object geoValue) {
        if (geoValue == null) {
            return null;
        }
        try {
            if (geoValue instanceof String text) {
                if (StringUtils.isBlank(text)) {
                    return null;
                }
                return objectMapper.readTree(text);
            }
            return objectMapper.valueToTree(geoValue);
        } catch (Exception e) {
            return null;
        }
    }

    private List<List<double[]>> extractPolygons(JsonNode geoNode) {
        List<List<double[]>> polygons = new ArrayList<>();
        String rootType = geoNode.path("type").asText("");
        if ("FeatureCollection".equalsIgnoreCase(rootType)) {
            JsonNode features = geoNode.path("features");
            if (features.isArray()) {
                for (JsonNode featureNode : features) {
                    collectPolygons(featureNode.path("geometry"), polygons);
                }
            }
            return polygons;
        }
        if ("Feature".equalsIgnoreCase(rootType)) {
            collectPolygons(geoNode.path("geometry"), polygons);
            return polygons;
        }
        collectPolygons(geoNode, polygons);
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
        List<double[]> points = new ArrayList<>();
        if (ringNode == null || !ringNode.isArray()) {
            return points;
        }
        for (JsonNode point : ringNode) {
            if (!point.isArray() || point.size() < 2) {
                continue;
            }
            points.add(new double[] {point.get(0).asDouble(), point.get(1).asDouble()});
        }
        return points;
    }

    private boolean isPointInPolygon(double x, double y, List<double[]> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i)[0];
            double yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0];
            double yj = polygon.get(j)[1];
            boolean intersects =
                    ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / ((yj - yi) + 1e-9) + xi);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private record ScoredCandidate(
            Lifeguard lifeguard,
            boolean inZone,
            boolean inFence,
            long activeAlertCount,
            long reportTimeMillis) {}
}
