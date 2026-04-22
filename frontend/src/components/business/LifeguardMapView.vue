<template>
  <div class="lifeguard-map-wrap">
    <div v-if="!keyReady" class="map-no-key">
      <div class="map-no-key__icon">
        <el-icon :size="36"><Location /></el-icon>
      </div>
      <div class="map-no-key__title">地图 API Key 未配置</div>
      <div class="map-no-key__desc">
        请在 <code>.env.development</code> 中设置
        <code>VITE_AMAP_KEY</code>，<br />
        前往
        <a href="https://console.amap.com/" target="_blank" rel="noopener">
          高德开放平台
        </a>
        申请 Web 端 JS API Key。
      </div>
    </div>

    <template v-else>
      <div ref="mapEl" class="amap-container" />
      <div v-if="mapLoading" class="map-loading-mask">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <span>地图加载中…</span>
      </div>

      <div class="map-legend">
        <span class="legend-item legend-item--online">在岗</span>
        <span class="legend-item legend-item--off">离岗</span>
        <span class="legend-item legend-item--fence">围栏外</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
import { Location, Loading } from "@element-plus/icons-vue";
import { useAMap } from "@/composables/useAMap";
import { recentLocations } from "@/api/lifeguardController";
import { listVenueFenceByBounds } from "@/api/venueController";
import { unwrapApiData } from "@/services/serviceUtils";
import type { LifeguardRecord } from "@/types/business";

interface Props {
  lifeguards: LifeguardRecord[];
}

const props = withDefaults(defineProps<Props>(), {
  lifeguards: () => [],
});

const { loadAMap, hasKey } = useAMap();
const keyReady = hasKey();

const mapEl = ref<HTMLDivElement | null>(null);
const mapLoading = ref(true);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let mapInstance: any = null;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const markerMap = new Map<string, any>();
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const fencePolygonMap = new Map<string, any[]>();
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const fenceLabelMap = new Map<string, any>();

let boundsLoadTimer: ReturnType<typeof setTimeout> | null = null;
let boundsRequestSeq = 0;

const STATUS_COLOR: Record<string, string> = {
  on_duty: "#22c55e",
  off_duty: "#f59e0b",
  out_of_fence: "#ef4444",
};

const STATUS_LABEL: Record<string, string> = {
  on_duty: "在岗",
  off_duty: "离岗",
  out_of_fence: "围栏外",
};

const toFeatureCollection = (
  rawGeoJson: unknown,
): {
  type: "FeatureCollection";
  features: Array<Record<string, any>>;
} | null => {
  if (!rawGeoJson) {
    return null;
  }
  let parsed: unknown = rawGeoJson;
  if (typeof rawGeoJson === "string") {
    if (!rawGeoJson.trim()) {
      return null;
    }
    try {
      parsed = JSON.parse(rawGeoJson);
    } catch {
      return null;
    }
  }
  if (!parsed || typeof parsed !== "object") {
    return null;
  }
  const node = parsed as Record<string, any>;
  if (node.type === "FeatureCollection" && Array.isArray(node.features)) {
    return {
      type: "FeatureCollection",
      features: node.features,
    };
  }
  if (node.type === "Feature") {
    return {
      type: "FeatureCollection",
      features: [node],
    };
  }
  if (
    (node.type === "Polygon" || node.type === "MultiPolygon") &&
    Array.isArray(node.coordinates)
  ) {
    return {
      type: "FeatureCollection",
      features: [{ type: "Feature", geometry: node, properties: {} }],
    };
  }
  return null;
};

const normalizePath = (path: unknown): number[][] => {
  if (!Array.isArray(path)) {
    return [];
  }
  return path
    .filter((point) => Array.isArray(point) && point.length >= 2)
    .map((point) => [Number(point[0]), Number(point[1])])
    .filter(([lng, lat]) => Number.isFinite(lng) && Number.isFinite(lat));
};

const geometryToPaths = (
  geometry: Record<string, any> | undefined,
): number[][][] => {
  if (!geometry || typeof geometry !== "object") {
    return [];
  }
  if (geometry.type === "Polygon" && Array.isArray(geometry.coordinates)) {
    const firstRing = normalizePath(geometry.coordinates[0]);
    return firstRing.length >= 3 ? [firstRing] : [];
  }
  if (geometry.type === "MultiPolygon" && Array.isArray(geometry.coordinates)) {
    return geometry.coordinates
      .map((polygon: unknown) =>
        Array.isArray(polygon) ? normalizePath((polygon as unknown[])[0]) : [],
      )
      .filter((path: number[][]) => path.length >= 3);
  }
  return [];
};

const resolveTopLeftPoint = (paths: number[][][]): [number, number] | null => {
  let minLng = Number.POSITIVE_INFINITY;
  let maxLat = Number.NEGATIVE_INFINITY;
  paths.forEach((path) => {
    path.forEach(([lng, lat]) => {
      if (lng < minLng) {
        minLng = lng;
      }
      if (lat > maxLat) {
        maxLat = lat;
      }
    });
  });
  if (!Number.isFinite(minLng) || !Number.isFinite(maxLat)) {
    return null;
  }
  return [minLng, maxLat];
};

const focusVenueFence = (venueId: string) => {
  if (!mapInstance) {
    return;
  }
  const polygons = fencePolygonMap.get(venueId) || [];
  if (!polygons.length) {
    return;
  }
  mapInstance.setFitView(polygons, false, [80, 80, 80, 80]);
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function upsertVenueFenceOverlay(AMap: any, venue: API.VenueVO) {
  if (venue.id == null || !mapInstance) {
    return;
  }
  const venueId = String(venue.id);
  const collection = toFeatureCollection(venue.fenceGeoJson);
  if (!collection) {
    return;
  }
  const paths = collection.features.flatMap((feature) =>
    geometryToPaths(feature.geometry),
  );
  if (!paths.length) {
    return;
  }

  if (fencePolygonMap.has(venueId)) {
    fencePolygonMap.get(venueId)?.forEach((polygon) => polygon.setMap(null));
    fencePolygonMap.delete(venueId);
  }
  if (fenceLabelMap.has(venueId)) {
    fenceLabelMap.get(venueId)?.setMap(null);
    fenceLabelMap.delete(venueId);
  }

  const polygons = paths.map((path) => {
    const polygon = new AMap.Polygon({
      path,
      fillColor: "#1b4f9b",
      fillOpacity: 0.12,
      strokeColor: "#1b4f9b",
      strokeWeight: 2,
    });
    polygon.on("click", () => {
      focusVenueFence(venueId);
    });
    polygon.setMap(mapInstance);
    return polygon;
  });
  fencePolygonMap.set(venueId, polygons);

  const labelAnchor = resolveTopLeftPoint(paths);
  if (labelAnchor) {
    const labelMarker = new AMap.Marker({
      position: labelAnchor,
      content: `<div style="padding:2px 8px;border-radius:10px;background:rgba(27,79,155,0.9);color:#fff;font-size:12px;line-height:20px;white-space:nowrap;">${venue.venueName || `${venue.id}号场馆`}</div>`,
      offset: new AMap.Pixel(8, -8),
      clickable: true,
    });
    labelMarker.on("click", () => {
      focusVenueFence(venueId);
    });
    labelMarker.setMap(mapInstance);
    fenceLabelMap.set(venueId, labelMarker);
  }
}

function clearFenceOverlays() {
  fencePolygonMap.forEach((polygons) => {
    polygons.forEach((polygon) => polygon.setMap(null));
  });
  fencePolygonMap.clear();
  fenceLabelMap.forEach((labelMarker) => labelMarker.setMap(null));
  fenceLabelMap.clear();
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function loadFenceByCurrentBounds(AMap: any) {
  if (!mapInstance) {
    return;
  }
  const bounds = mapInstance.getBounds?.();
  if (!bounds) {
    return;
  }
  const southWest = bounds.getSouthWest?.();
  const northEast = bounds.getNorthEast?.();
  if (!southWest || !northEast) {
    return;
  }
  const requestSeq = ++boundsRequestSeq;
  const response = await listVenueFenceByBounds({
    current: 1,
    pageSize: 100,
    minLng: Number(southWest.lng),
    maxLng: Number(northEast.lng),
    minLat: Number(southWest.lat),
    maxLat: Number(northEast.lat),
    status: 1,
  });
  if (requestSeq !== boundsRequestSeq) {
    return;
  }
  const pageData = unwrapApiData<API.PageVenueVO>(response, "加载场馆围栏失败");
  clearFenceOverlays();
  (pageData.records || []).forEach((venue) => {
    upsertVenueFenceOverlay(AMap, venue);
  });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function scheduleLoadFenceByBounds(AMap: any) {
  if (boundsLoadTimer) {
    clearTimeout(boundsLoadTimer);
  }
  boundsLoadTimer = setTimeout(() => {
    void loadFenceByCurrentBounds(AMap);
  }, 300);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function buildMarkerContent(record: LifeguardRecord): string {
  const color = STATUS_COLOR[record.dutyStatus] ?? "#64748b";
  return `
    <div style="
      background:${color};
      color:#fff;
      border-radius:50%;
      width:32px;height:32px;
      display:flex;align-items:center;justify-content:center;
      font-size:12px;font-weight:600;
      border:2px solid #fff;
      box-shadow:0 2px 6px rgba(0,0,0,0.25);
      cursor:pointer;
    " title="${record.name}">
      ${record.name.slice(-1)}
    </div>
  `;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function refreshMarkers(AMap: any) {
  if (!mapInstance || !props.lifeguards.length) return;

  const tasks = props.lifeguards.map(async (guard) => {
    try {
      const resp = await recentLocations({ lifeguardId: Number(guard.id) });
      const logs = unwrapApiData<API.LifeguardLocationLogVO[]>(resp, "");
      const latest = logs?.[0];
      if (!latest?.longitude || !latest?.latitude) return;

      const lnglat = new AMap.LngLat(latest.longitude, latest.latitude);

      if (markerMap.has(guard.id)) {
        const existing = markerMap.get(guard.id);
        existing.setPosition(lnglat);
        existing.setContent(buildMarkerContent(guard));
      } else {
        const marker = new AMap.Marker({
          position: lnglat,
          content: buildMarkerContent(guard),
          offset: new AMap.Pixel(-16, -16),
          title: guard.name,
        });

        const infoWindow = new AMap.InfoWindow({
          content: `
            <div style="padding:8px 12px;min-width:160px;">
              <div style="font-weight:600;margin-bottom:4px;">${guard.name}</div>
              <div style="font-size:12px;color:#666;">状态：${STATUS_LABEL[guard.dutyStatus] ?? "未知"}</div>
              <div style="font-size:12px;color:#666;">场馆：${guard.venue}</div>
              <div style="font-size:12px;color:#666;">最近上报：${guard.lastReportTime}</div>
            </div>
          `,
          offset: new AMap.Pixel(0, -36),
        });

        marker.on("click", () => {
          infoWindow.open(mapInstance, marker.getPosition());
        });

        marker.setMap(mapInstance);
        markerMap.set(guard.id, marker);
      }
    } catch {
      // 某个救生员无定位数据时静默跳过
    }
  });

  await Promise.allSettled(tasks);

  const positions = [...markerMap.values()]
    .map((m) => m.getPosition())
    .filter(Boolean);

  if (positions.length > 0) {
    mapInstance.setFitView([...markerMap.values()], false, [60, 60, 60, 60]);
  }
}

function clearStaleMarkers() {
  const currentIds = new Set(props.lifeguards.map((g) => g.id));
  for (const [id, marker] of markerMap.entries()) {
    if (!currentIds.has(id)) {
      marker.setMap(null);
      markerMap.delete(id);
    }
  }
}

async function initMap() {
  if (!mapEl.value || !keyReady) return;

  try {
    const AMap = await loadAMap();
    mapLoading.value = false;

    mapInstance = new AMap.Map(mapEl.value, {
      zoom: 16,
      center: [116.397428, 39.90923],
      mapStyle: "amap://styles/fresh",
    });

    mapInstance.on("moveend", () => {
      scheduleLoadFenceByBounds(AMap);
    });
    mapInstance.on("zoomend", () => {
      scheduleLoadFenceByBounds(AMap);
    });

    await refreshMarkers(AMap);
    await loadFenceByCurrentBounds(AMap);
  } catch (e) {
    mapLoading.value = false;
    console.error("[LifeguardMapView] 地图初始化失败", e);
  }
}

watch(
  () => props.lifeguards,
  async () => {
    if (!mapInstance) return;
    clearStaleMarkers();
    const AMap = await loadAMap();
    await refreshMarkers(AMap);
  },
  { deep: false },
);

onMounted(() => {
  if (keyReady) initMap();
});

onBeforeUnmount(() => {
  if (boundsLoadTimer) {
    clearTimeout(boundsLoadTimer);
    boundsLoadTimer = null;
  }
  clearFenceOverlays();
  if (mapInstance) {
    mapInstance.destroy();
    mapInstance = null;
  }
});
</script>

<style scoped>
.lifeguard-map-wrap {
  position: relative;
  width: 100%;
  height: 520px;
  border-radius: 6px;
  overflow: hidden;
}

.amap-container {
  width: 100%;
  height: 100%;
}

.map-loading-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(244, 247, 251, 0.85);
  font-size: 13px;
  color: #606266;
  pointer-events: none;
}

.map-no-key {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #f4f7fb 0%, #edf2f8 100%);
  border: 1px dashed #c8d5e6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #476b9d;
  padding: 24px;
  box-sizing: border-box;
}

.map-no-key__icon {
  opacity: 0.6;
}

.map-no-key__title {
  font-size: 16px;
  font-weight: 600;
}

.map-no-key__desc {
  font-size: 13px;
  color: #64748b;
  text-align: center;
  line-height: 1.8;
}

.map-no-key__desc a {
  color: #1b4f9b;
  text-decoration: underline;
}

.map-no-key__desc code {
  background: #e8eff9;
  border-radius: 3px;
  padding: 1px 5px;
  font-size: 12px;
}

.map-legend {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  gap: 10px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 5px 10px;
  font-size: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.legend-item::before {
  content: "";
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-item--online::before {
  background: #22c55e;
}
.legend-item--off::before {
  background: #f59e0b;
}
.legend-item--fence::before {
  background: #ef4444;
}
</style>
