<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑设备' : '新增设备'"
    width="660px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
      <el-form-item label="设备名称" prop="cameraName">
        <el-input v-model="form.cameraName" placeholder="请输入设备名称" />
      </el-form-item>
      <el-form-item label="所属场馆" prop="venueId">
        <el-select
          v-model="form.venueId"
          filterable
          :remote="!isEdit"
          reserve-keyword
          :loading="venueLoading"
          :remote-method="handleVenueRemoteSearch"
          style="width: 100%"
          @visible-change="handleVenueVisibleChange"
          @popup-scroll="handleVenuePopupScroll"
        >
          <el-option
            v-for="option in mergedVenueOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="区域ID" prop="zoneId">
        <el-input-number
          v-model="form.zoneId"
          :min="1"
          placeholder="可选"
          style="width: 100%"
          :controls="false"
        />
      </el-form-item>
      <el-form-item label="设备编码" prop="cameraCode">
        <el-input
          v-model="form.cameraCode"
          :disabled="isEdit"
          placeholder="新增时可自定义，留空则自动生成"
        />
      </el-form-item>
      <el-form-item label="传输协议" prop="protocol">
        <el-select v-model="form.protocol" style="width: 100%">
          <el-option label="HTTP" value="HTTP" />
          <el-option label="RTSP" value="RTSP" />
          <el-option label="HTTP-FLV" value="HTTP-FLV" />
        </el-select>
      </el-form-item>
      <el-form-item label="视频流地址" prop="streamUrl">
        <el-input v-model="form.streamUrl" placeholder="请输入视频流地址" />
      </el-form-item>
      <el-form-item label="设备状态" prop="deviceStatus">
        <el-select v-model="form.deviceStatus" style="width: 100%">
          <el-option label="在线" value="ONLINE" />
          <el-option label="离线" value="OFFLINE" />
          <el-option label="异常" value="ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item label="健康状态" prop="healthStatus">
        <el-select v-model="form.healthStatus" style="width: 100%">
          <el-option label="正常" value="NORMAL" />
          <el-option label="告警" value="WARN" />
          <el-option label="故障" value="ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch
          v-model="enabledBool"
          active-text="启用"
          inactive-text="禁用"
        />
      </el-form-item>
      <el-form-item label="画面旋转">
        <el-select v-model="form.rotation" style="width: 100%">
          <el-option label="不旋转" :value="0" />
          <el-option label="顺时针 90" :value="90" />
          <el-option label="180" :value="180" />
          <el-option label="顺时针 270" :value="270" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit"
        >保存</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import {
  addCameraDevice,
  getCameraDeviceVoById,
  updateCameraDevice,
} from "@/api/cameraDeviceController";
import { getVenueVoById } from "@/api/venueController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";

interface FormModel {
  cameraName: string;
  venueId: number;
  zoneId: number | undefined;
  cameraCode: string;
  protocol: string;
  streamUrl: string;
  deviceStatus: string;
  healthStatus: string;
  enabled: number;
  rotation: number;
}interface Props {
  modelValue: boolean;
  deviceId?: number;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const loading = ref(false);
const isEdit = computed(() => props.deviceId != null && props.deviceId > 0);
// 用于编辑模式下存储当前场馆信息，确保正确显示场馆名称
const currentVenueInfo = ref<{ id: number; name: string } | null>(null);
const {
  venueOptions,
  venueLoading,
  loadNextPage,
  handleVenueRemoteSearch,
  handleVenueVisibleChange,
  handleVenuePopupScroll,
  ensureVenueOption,
} = useVenueRemoteSelect<number>({
  valueType: "number",
  errorMessage: "获取场馆列表失败",
});

// 合并后的场馆选项列表（包含当前选中的场馆）
const mergedVenueOptions = computed(() => {
  const options = [...venueOptions.value];
  // 如果当前场馆信息存在且不在选项列表中，则添加到开头
  if (currentVenueInfo.value) {
    const curId = Number(currentVenueInfo.value.id);
    const exists = options.some(
      (opt) => Number(opt.value) === curId,
    );
    if (!exists) {
      options.unshift({
        label: currentVenueInfo.value.name,
        value: currentVenueInfo.value.id,
      });
    }
  }
  return options;
});

const form = reactive<FormModel>({
  cameraName: "",
  venueId: 0,
  zoneId: undefined,
  cameraCode: "",
  protocol: "RTSP",
  streamUrl: "",
  deviceStatus: "ONLINE",
  healthStatus: "NORMAL",
  enabled: 1,
  rotation: 0,
});

const enabledBool = computed({
  get: () => form.enabled === 1,
  set: (val: boolean) => {
    form.enabled = val ? 1 : 0;
  },
});

const formRules = {
  cameraName: [{ required: true, message: "请输入设备名称", trigger: "blur" }],
  venueId: [{ required: true, message: "请选择所属场馆", trigger: "change" }],
  protocol: [{ required: true, message: "请选择传输协议", trigger: "change" }],
  streamUrl: [{ required: true, message: "请输入视频流地址", trigger: "blur" }],
  deviceStatus: [
    { required: true, message: "请选择设备状态", trigger: "change" },
  ],
  healthStatus: [
    { required: true, message: "请选择健康状态", trigger: "change" },
  ],
};

const resetForm = () => {
  formRef.value?.resetFields();
  currentVenueInfo.value = null;
  form.cameraName = "";
  form.venueId = venueOptions.value[0]?.value ?? 0;
  form.zoneId = undefined;
  form.cameraCode = "";
  form.protocol = "RTSP";
  form.streamUrl = "";
  form.deviceStatus = "ONLINE";
  form.healthStatus = "NORMAL";
  form.enabled = 1;
  form.rotation = 0;
};

onMounted(async () => {
  await loadNextPage();
  if (!isEdit.value && form.venueId <= 0) {
    form.venueId = venueOptions.value[0]?.value ?? 0;
  }
});

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && !venueOptions.value.length) {
      await loadNextPage();
      if (!isEdit.value && form.venueId <= 0) {
        form.venueId = venueOptions.value[0]?.value ?? 0;
      }
    }
  },
);

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      if (isEdit.value && props.deviceId) {
        await loadDeviceData();
      } else {
        resetForm();
      }
    }
  },
);

const loadDeviceData = async () => {
  if (!props.deviceId) return;
  try {
    loading.value = true;
    const response = await getCameraDeviceVoById({ id: props.deviceId });
    const data = unwrapApiData<API.CameraDeviceVO>(
      response,
      "获取设备详情失败",
    );
    // 接口可能返回字符串类型的 Long，与 el-option 的 number 不一致会导致只显示 ID
    const normalizedVenueId = Number(data.venueId ?? 0);

    if (normalizedVenueId > 0) {
      try {
        const venueResponse = await getVenueVoById({ id: normalizedVenueId });
        const venue = unwrapApiData<API.VenueVO>(venueResponse, "获取场馆详情失败");
        if (venue?.id != null) {
          currentVenueInfo.value = {
            id: Number(venue.id),
            name: venue.venueName || `${venue.id}号场馆`,
          };
        }
      } catch {
        currentVenueInfo.value = {
          id: normalizedVenueId,
          name: `${normalizedVenueId}号场馆`,
        };
      }
    } else {
      currentVenueInfo.value = null;
    }
    await ensureVenueOption(normalizedVenueId > 0 ? normalizedVenueId : undefined);
    // 等待 Vue 更新 DOM，确保 el-select 的 options 已同步
    await nextTick();
    form.cameraName = data.cameraName || "";
    form.venueId =
      Number.isFinite(normalizedVenueId) && normalizedVenueId > 0
        ? normalizedVenueId
        : venueOptions.value[0]?.value || 0;
    form.zoneId = data.zoneId ?? undefined;
    form.cameraCode = data.cameraCode || "";
    form.protocol = data.protocol || "RTSP";
    form.streamUrl = data.streamUrl || "";
    form.deviceStatus = data.deviceStatus || "ONLINE";
    form.healthStatus = data.healthStatus || "NORMAL";
    form.enabled = data.enabled ?? 1;
    form.rotation = data.rotation ?? 0;
    // 再次等待，确保 el-select 能正确匹配选项并显示 label
    await nextTick();
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
};

const handleCancel = () => {
  emit("update:modelValue", false);
  resetForm();
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    loading.value = true;
    const normalizedZoneId =
      form.zoneId != null && Number(form.zoneId) > 0
        ? Number(form.zoneId)
        : undefined;

    if (isEdit.value && props.deviceId) {
      const response = await updateCameraDevice({
        id: props.deviceId,
        venueId: form.venueId,
        zoneId: normalizedZoneId,
        cameraCode: form.cameraCode || undefined,
        cameraName: form.cameraName,
        streamUrl: form.streamUrl,
        protocol: form.protocol,
        deviceStatus: form.deviceStatus,
        healthStatus: form.healthStatus,
        enabled: form.enabled,
        rotation: form.rotation,
      });
      unwrapApiData<boolean>(response, "更新设备失败");
      ElMessage.success("编辑设备成功");
    } else {
      const venuePrefix = String(form.venueId || "V")
        .slice(0, 1)
        .toUpperCase();
      const response = await addCameraDevice({
        venueId: form.venueId,
        zoneId: normalizedZoneId,
        cameraCode: form.cameraCode || `${venuePrefix}-${Date.now()}`,
        cameraName: form.cameraName,
        streamUrl: form.streamUrl,
        protocol: form.protocol,
        deviceStatus: form.deviceStatus,
        healthStatus: form.healthStatus,
        enabled: form.enabled,
        rotation: form.rotation,
      });
      unwrapApiData<number>(response, "新增设备失败");
      ElMessage.success("新增设备成功");
    }

    emit("update:modelValue", false);
    emit("success");
    resetForm();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    } else {
      ElMessage.warning("请检查表单填写是否完整");
    }
  } finally {
    loading.value = false;
  }
};
</script>
