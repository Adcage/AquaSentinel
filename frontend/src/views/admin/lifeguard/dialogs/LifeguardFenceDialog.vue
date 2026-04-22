<template>
  <el-dialog
    :model-value="modelValue"
    title="围栏配置"
    width="1100px"
    @close="handleClose"
    @opened="onDialogOpened"
  >
    <div class="fence-wrap">
      <div class="fence-toolbar">
        <el-select
          v-model="targetVenueId"
          class="venue-select"
          placeholder="请选择场馆"
          filterable
          remote
          reserve-keyword
          :loading="venuesLoading"
          :remote-method="handleVenueRemoteSearch"
          @visible-change="handleVenueVisibleChange"
          @popup-scroll="handleVenuePopupScroll"
        >
          <el-option
            v-for="item in venueOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-tag type="info" effect="plain"
          >当前场馆：{{ currentVenueLabel }}</el-tag
        >

        <el-button
          v-if="!drawing"
          type="primary"
          plain
          :disabled="!targetVenueId || !mapReady"
          @click="startDraw"
        >
          {{ hasFence ? "新增区域" : "绘制围栏" }}
        </el-button>
        <el-button v-else type="warning" @click="cancelDraw"
          >取消绘制</el-button
        >

        <el-button :disabled="!hasFence" @click="clearFence">
          清除围栏
        </el-button>

        <el-tag v-if="hasFence" type="success" size="small">已有围栏</el-tag>
        <el-tag v-else type="info" size="small">暂无围栏</el-tag>
      </div>

      <div v-if="!mapReady" class="fence-no-key">
        <el-text type="info">配置 VITE_AMAP_KEY 后可使用围栏配置功能</el-text>
      </div>
      <div v-else ref="mapEl" class="fence-map" />

      <div v-if="drawing" class="fence-hint">
        <el-icon><InfoFilled /></el-icon>
        在地图上依次单击围栏顶点，<strong>双击</strong>完成绘制
      </div>
      <div
        v-else-if="mapReady && targetVenueId"
        class="fence-hint fence-hint--idle"
      >
        点击"绘制围栏"开始在地图上圈定区域
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!hasFence || !targetVenueId"
        @click="saveFence"
      >
        保存围栏
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { InfoFilled } from "@element-plus/icons-vue";
import { useAMap } from "@/composables/useAMap";
import {
  getVenueVoById,
  listVenueFenceByBounds,
  updateVenue,
} from "@/api/venueController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";

interface Props {
  modelValue: boolean;
  venueId?: string;
}

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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function createPolygon(AMap: any, path: number[][]) {
  return new AMap.Polygon({
    path,
    fillColor: "#1b4f9b",
    fillOpacity: 0.15,
    strokeColor: "#1b4f9b",
    strokeWeight: 2,
  });
}

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

const collectPathsFromCollection = (collection: {
  type: "FeatureCollection";
  features: Array<Record<string, any>>;
}): number[][][] => {
  return collection.features.flatMap((feature) =>
    geometryToPaths(feature.geometry),
  );
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function clearActiveFenceLabel() {
  if (activeFenceLabel && typeof activeFenceLabel.setMap === "function") {
    activeFenceLabel.setMap(null);
  }
  activeFenceLabel = null;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function setActiveFenceLabel(AMap: any, paths: number[][][]) {
  clearActiveFenceLabel();
  if (!mapInstance || !paths.length) {
    return;
  }
  const topLeftPoint = resolveTopLeftPoint(paths);
  if (!topLeftPoint) {
    return;
  }
  const labelText = currentVenueLabel.value || "当前场馆";
  activeFenceLabel = new AMap.Marker({
    position: topLeftPoint,
    content: `<div style="padding:2px 8px;border-radius:10px;background:rgba(27,79,155,0.95);color:#fff;font-size:12px;line-height:20px;white-space:nowrap;">${labelText}</div>`,
    offset: new AMap.Pixel(8, -8),
    clickable: false,
  });
  activeFenceLabel.setMap(mapInstance);
}

function clearPassiveFenceOverlays() {
  passiveFencePolygonMap.forEach((polygons) => {
    polygons.forEach((polygon) => polygon.setMap(null));
  });
  passiveFencePolygonMap.clear();
  passiveFenceLabelMap.forEach((label) => label.setMap(null));
  passiveFenceLabelMap.clear();
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function renderPassiveVenueFence(AMap: any, venue: API.VenueVO) {
  if (venue.id == null || !mapInstance) {
    return;
  }
  const venueId = String(venue.id);
  const collection = toFeatureCollection(venue.fenceGeoJson);
  if (!collection) {
    return;
  }
  const paths = collectPathsFromCollection(collection);
  if (!paths.length) {
    return;
  }
  const polygons = paths.map((path) => {
    const polygon = new AMap.Polygon({
      path,
      fillColor: "#94a3b8",
      fillOpacity: 0.08,
      strokeColor: "#64748b",
      strokeWeight: 2,
      strokeStyle: "dashed",
    });
    polygon.setMap(mapInstance);
    return polygon;
  });
  passiveFencePolygonMap.set(venueId, polygons);

  const topLeftPoint = resolveTopLeftPoint(paths);
  if (!topLeftPoint) {
    return;
  }
  const labelMarker = new AMap.Marker({
    position: topLeftPoint,
    content: `<div style="padding:2px 8px;border-radius:10px;background:rgba(71,85,105,0.9);color:#fff;font-size:12px;line-height:20px;white-space:nowrap;">${venue.venueName || `${venue.id}号场馆`}</div>`,
    offset: new AMap.Pixel(8, -8),
    clickable: false,
  });
  labelMarker.setMap(mapInstance);
  passiveFenceLabelMap.set(venueId, labelMarker);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function loadVisibleVenueFenceByBounds(AMap?: any) {
  if (!mapInstance || !mapReady) {
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
  try {
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
    const pageData = unwrapApiData<API.PageVenueVO>(
      response,
      "加载场馆围栏失败",
    );
    const resolvedAMap = AMap ?? (await loadAMap());
    clearPassiveFenceOverlays();
    (pageData.records || []).forEach((venue) => {
      if (targetVenueId.value && String(venue.id) === targetVenueId.value) {
        return;
      }
      renderPassiveVenueFence(resolvedAMap, venue);
    });
  } catch (e) {
    if (e instanceof Error) {
      ElMessage.error(e.message);
    }
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function scheduleLoadVisibleVenueFenceByBounds(AMap?: any) {
  if (boundsLoadTimer) {
    clearTimeout(boundsLoadTimer);
  }
  boundsLoadTimer = setTimeout(() => {
    void loadVisibleVenueFenceByBounds(AMap);
  }, 300);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function renderFenceCollection(
  AMap: any,
  collection: {
    type: "FeatureCollection";
    features: Array<Record<string, any>>;
  },
) {
  clearFenceLayer();
  fenceGeoJson = {
    type: "FeatureCollection",
    features: [],
  };
  const allPaths: number[][][] = [];

  collection.features.forEach((feature) => {
    const paths = geometryToPaths(feature.geometry);
    paths.forEach((path) => {
      allPaths.push(path);
      const polygon = createPolygon(AMap, path);
      polygon.setMap(mapInstance);
      fencePolygons.push(polygon);
      fenceGeoJson?.features.push({
        type: "Feature",
        geometry: { type: "Polygon", coordinates: [path] },
        properties: feature.properties || {},
      });
    });
  });

  hasFence.value = fencePolygons.length > 0;
  if (hasFence.value) {
    setActiveFenceLabel(AMap, allPaths);
    mapInstance?.setFitView(fencePolygons, false, [40, 40, 40, 40]);
  } else {
    clearActiveFenceLabel();
  }
}

const props = withDefaults(defineProps<Props>(), {
  venueId: "",
});

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  saved: [];
}>();

const { loadAMap, hasKey } = useAMap();
const mapReady = hasKey();

const mapEl = ref<HTMLDivElement | null>(null);
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let mapInstance: any = null;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let amapNs: any = null;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let mouseTool: any = null;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let fencePolygons: any[] = [];
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let activeFenceLabel: any = null;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const passiveFencePolygonMap = new Map<string, any[]>();
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const passiveFenceLabelMap = new Map<string, any>();
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let fenceGeoJson: {
  type: "FeatureCollection";
  features: Array<Record<string, any>>;
} | null = null;

let boundsLoadTimer: ReturnType<typeof setTimeout> | null = null;
let boundsRequestSeq = 0;

const targetVenueId = ref("");
const {
  venueOptions,
  venueLoading: venuesLoading,
  loadNextPage,
  handleVenueRemoteSearch,
  handleVenueVisibleChange,
  handleVenuePopupScroll,
  ensureVenueOption,
} = useVenueRemoteSelect<string>({
  valueType: "string",
  status: 1,
  errorMessage: "加载场馆列表失败",
});
const drawing = ref(false);
const hasFence = ref(false);
const saving = ref(false);

const currentVenueLabel = computed(() => {
  return (
    venueOptions.value.find((item) => item.value === targetVenueId.value)
      ?.label || "未选择场馆"
  );
});

watch(
  () => props.modelValue,
  async (val) => {
    if (val) {
      if (props.venueId) {
        targetVenueId.value = props.venueId;
        await ensureVenueOption(props.venueId);
      }
    }
  },
);

watch(
  () => props.venueId,
  async (val) => {
    if (val) {
      targetVenueId.value = val;
      await ensureVenueOption(val);
    }
  },
);

watch(targetVenueId, async (val, oldVal) => {
  if (val === oldVal) {
    return;
  }
  if (!props.modelValue || !mapReady || !mapInstance) {
    return;
  }
  if (val) {
    await loadExistingFence();
  } else {
    cancelDraw();
    clearFenceLayer();
  }
  await loadVisibleVenueFenceByBounds();
});

async function initMap() {
  if (!mapEl.value || !mapReady) return;
  try {
    const AMap = await loadAMap();
    amapNs = AMap;
    mapInstance = new AMap.Map(mapEl.value, {
      zoom: 16,
      center: [116.397428, 39.90923],
      mapStyle: "amap://styles/fresh",
    });
    mouseTool = new AMap.MouseTool(mapInstance);
    mouseTool.on("draw", onDrawEnd);
    mapInstance.on("moveend", () => {
      scheduleLoadVisibleVenueFenceByBounds(AMap);
    });
    mapInstance.on("zoomend", () => {
      scheduleLoadVisibleVenueFenceByBounds(AMap);
    });
    if (targetVenueId.value) await loadExistingFence(AMap);
    await loadVisibleVenueFenceByBounds(AMap);
  } catch (e) {
    console.error("[FenceDialog] 地图初始化失败", e);
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function loadExistingFence(AMap?: any) {
  if (!targetVenueId.value || !mapInstance) return;
  cancelDraw();
  clearFenceLayer();

  try {
    const resp = await getVenueVoById({ id: Number(targetVenueId.value) });
    const vo = unwrapApiData<API.VenueVO>(resp, "");
    const featureCollection = toFeatureCollection(vo?.fenceGeoJson);
    if (!featureCollection) return;

    const resolvedAMap = AMap ?? (await loadAMap());
    renderFenceCollection(resolvedAMap, featureCollection);
  } catch {
    // 无围栏数据时静默
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onDrawEnd(e: any) {
  drawing.value = false;
  mouseTool?.close(false);

  const polygon = e.obj;
  if (!polygon) {
    return;
  }
  const path: number[][] = polygon
    .getPath()
    .map((lnglat: any) => [Number(lnglat.lng), Number(lnglat.lat)]);
  if (path.length < 3) {
    polygon.setMap(null);
    return;
  }
  fencePolygons.push(polygon);
  hasFence.value = true;
  if (!fenceGeoJson) {
    fenceGeoJson = { type: "FeatureCollection", features: [] };
  }
  fenceGeoJson.features.push({
    type: "Feature",
    geometry: { type: "Polygon", coordinates: [path] },
    properties: {},
  });

  polygon.setOptions({
    fillColor: "#1b4f9b",
    fillOpacity: 0.15,
    strokeColor: "#1b4f9b",
    strokeWeight: 2,
  });

  const allPaths = collectPathsFromCollection(fenceGeoJson);
  if (amapNs) {
    setActiveFenceLabel(amapNs, allPaths);
  }
}

function startDraw() {
  if (!mouseTool) return;
  drawing.value = true;
  mouseTool.polygon({
    fillColor: "#1b4f9b",
    fillOpacity: 0.12,
    strokeColor: "#1b4f9b",
    strokeWeight: 2,
    strokeStyle: "dashed",
  });
}

function cancelDraw() {
  drawing.value = false;
  mouseTool?.close(true);
}

function clearFenceLayer() {
  fencePolygons.forEach((polygon) => {
    if (polygon && typeof polygon.setMap === "function") {
      polygon.setMap(null);
    }
  });
  fencePolygons = [];
  clearActiveFenceLabel();
  hasFence.value = false;
  fenceGeoJson = null;
}

function clearFence() {
  clearFenceLayer();
}

async function saveFence() {
  if (!targetVenueId.value || !fenceGeoJson) return;
  saving.value = true;
  try {
    const resp = await updateVenue({
      id: Number(targetVenueId.value),
      fenceGeoJson,
    });
    unwrapApiData(resp, "保存围栏失败");
    ElMessage.success("围栏保存成功");
    emit("saved");
    handleClose();
  } catch (e) {
    if (e instanceof Error) ElMessage.error(e.message);
  } finally {
    saving.value = false;
  }
}

const onDialogOpened = async () => {
  await nextTick();
  await loadNextPage();
  await ensureVenueOption(targetVenueId.value);
  if (mapReady) await initMap();
};

const handleClose = () => {
  cancelDraw();
  if (boundsLoadTimer) {
    clearTimeout(boundsLoadTimer);
    boundsLoadTimer = null;
  }
  clearPassiveFenceOverlays();
  if (mapInstance) {
    mapInstance.destroy();
    mapInstance = null;
    amapNs = null;
    mouseTool = null;
    fencePolygons = [];
  }
  clearActiveFenceLabel();
  hasFence.value = false;
  fenceGeoJson = null;
  emit("update:modelValue", false);
};
</script>

<style scoped>
.fence-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fence-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.venue-select {
  width: 220px;
}

.fence-map {
  width: 100%;
  height: 520px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.fence-no-key {
  height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #e4e7ed;
  border-radius: 6px;
}

.fence-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 6px 12px;
}

.fence-hint--idle {
  color: #909399;
  background: #f4f4f5;
  border-color: #e9e9eb;
}
</style>
