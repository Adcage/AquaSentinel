<template>
  <el-dialog
    :model-value="modelValue"
    title="查看救生员轨迹"
    width="860px"
    @close="handleClose"
    @opened="onDialogOpened"
  >
    <div v-loading="loading">
      <div v-if="lifeguardInfo" class="lifeguard-info">
        <div class="info-item">
          <span class="label">姓名：</span>
          <span class="value">{{ lifeguardInfo.fullName }}</span>
        </div>
        <div class="info-item">
          <span class="label">所属场馆：</span>
          <span class="value">{{ venueLabel }}</span>
        </div>
      </div>

      <el-divider />

      <div v-if="mapReady" class="trajectory-map-wrap">
        <div ref="mapEl" class="trajectory-map" />
      </div>
      <div v-else class="map-no-key-tip">
        <el-text type="info" size="small">
          配置 VITE_AMAP_KEY 后可显示轨迹地图
        </el-text>
      </div>

      <el-divider />

      <div class="trajectory-list">
        <el-table :data="trajectoryData" border max-height="260">
          <el-table-column prop="reportedAt" label="上报时间" min-width="160" />
          <el-table-column label="经纬度" min-width="180">
            <template #default="scope">
              {{ scope.row.longitude }}, {{ scope.row.latitude }}
            </template>
          </el-table-column>
          <el-table-column label="围栏状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.inFence ? 'success' : 'danger'">
                {{ scope.row.inFence ? "围栏内" : "围栏外" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reportSource" label="上报来源" width="120" />
        </el-table>

        <div v-if="!trajectoryData.length && !loading" class="empty-state">
          暂无轨迹记录
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import { getLifeguardVoById, recentLocations } from "@/api/lifeguardController";
import { unwrapApiData, venueIdToName } from "@/services/serviceUtils";
import { useAMap } from "@/composables/useAMap";

interface TrajectoryRecord {
  reportedAt: string;
  longitude: number;
  latitude: number;
  inFence: boolean;
  reportSource: string;
}

interface Props {
  modelValue: boolean;
  lifeguardId?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
}>();

const loading = ref(false);
const lifeguardInfo = ref<API.LifeguardVO | null>(null);
const trajectoryData = ref<TrajectoryRecord[]>([]);

const { loadAMap, hasKey } = useAMap();
const mapReady = hasKey();
const mapEl = ref<HTMLDivElement | null>(null);
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let mapInstance: any = null;
let dialogOpened = false;

const venueLabel = computed(() => {
  return venueIdToName(lifeguardInfo.value?.venueId ?? null);
});

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && props.lifeguardId) {
      await loadData();
    }
  },
);

const loadData = async () => {
  if (!props.lifeguardId) return;

  try {
    loading.value = true;

    const lifeguardResponse = await getLifeguardVoById({
      id: Number(props.lifeguardId),
    });
    lifeguardInfo.value = unwrapApiData<API.LifeguardVO>(
      lifeguardResponse,
      "获取救生员信息失败",
    );

    const trajectoryResponse = await recentLocations({
      lifeguardId: Number(props.lifeguardId),
    });
    const locations = unwrapApiData<API.LifeguardLocationLogVO[]>(
      trajectoryResponse,
      "获取轨迹记录失败",
    );

    trajectoryData.value = (locations || []).map((item) => ({
      reportedAt: item.reportedAt || "-",
      longitude: item.longitude || 0,
      latitude: item.latitude || 0,
      inFence: item.inFence === 1,
      reportSource: item.reportSource || "-",
    }));

    if (dialogOpened) {
      await drawTrajectoryMap();
    }
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  } finally {
    loading.value = false;
  }
};

async function drawTrajectoryMap() {
  if (!mapReady || !mapEl.value || !trajectoryData.value.length) return;

  const points = trajectoryData.value
    .filter((r) => r.longitude && r.latitude)
    .map((r) => [r.longitude, r.latitude] as [number, number]);

  if (points.length === 0) return;

  try {
    const AMap = await loadAMap();

    if (!mapInstance) {
      await nextTick();
      mapInstance = new AMap.Map(mapEl.value, {
        zoom: 16,
        center: points[0],
        mapStyle: "amap://styles/fresh",
      });
    } else {
      mapInstance.clearMap();
    }

    const polyline = new AMap.Polyline({
      path: points,
      strokeColor: "#1b4f9b",
      strokeWeight: 3,
      strokeOpacity: 0.9,
      lineJoin: "round",
    });
    polyline.setMap(mapInstance);

    const startMarker = new AMap.Marker({
      position: points[0],
      content:
        '<div style="background:#22c55e;color:#fff;border-radius:50%;width:20px;height:20px;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:600;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,0.3);">起</div>',
      offset: new AMap.Pixel(-10, -10),
    });
    const endMarker = new AMap.Marker({
      position: points[points.length - 1],
      content:
        '<div style="background:#ef4444;color:#fff;border-radius:50%;width:20px;height:20px;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:600;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,0.3);">终</div>',
      offset: new AMap.Pixel(-10, -10),
    });
    startMarker.setMap(mapInstance);
    endMarker.setMap(mapInstance);

    mapInstance.setFitView(
      [polyline, startMarker, endMarker],
      false,
      [40, 40, 40, 40],
    );
  } catch (e) {
    console.error("[TrajectoryDialog] 轨迹地图渲染失败", e);
  }
}

const onDialogOpened = async () => {
  dialogOpened = true;
  if (trajectoryData.value.length) {
    await drawTrajectoryMap();
  }
};

const handleClose = () => {
  dialogOpened = false;
  if (mapInstance) {
    mapInstance.destroy();
    mapInstance = null;
  }
  emit("update:modelValue", false);
};
onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy();
    mapInstance = null;
  }
});
</script>

<style scoped>
.lifeguard-info {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item .label {
  font-weight: 600;
  color: #606266;
}

.info-item .value {
  color: #303133;
}

.trajectory-list {
  min-height: 200px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: #909399;
  font-size: 14px;
}

.trajectory-map-wrap {
  width: 100%;
  height: 260px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.trajectory-map {
  width: 100%;
  height: 100%;
}

.map-no-key-tip {
  padding: 8px 0;
  text-align: center;
}
</style>
