<template>
  <el-dialog
    :model-value="modelValue"
    title="指派处理"
    width="600px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <div v-if="alarmInfo" class="alarm-info">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="报警ID">
          {{ alarmInfo.id }}
        </el-descriptions-item>
        <el-descriptions-item label="报警类型">
          {{ getAlarmTypeLabel(alarmInfo.type) }}
        </el-descriptions-item>
        <el-descriptions-item label="触发时间" :span="2">
          {{ alarmInfo.triggerTime }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <el-divider />

    <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
      <el-form-item label="指派救生员" prop="lifeguardId">
        <el-select
          v-model="form.lifeguardId"
          v-loading="loadingLifeguards"
          placeholder="请选择救生员"
          style="width: 100%"
          filterable
        >
          <el-option
            v-for="lifeguard in lifeguardList"
            :key="lifeguard.id"
            :label="`${lifeguard.fullName} (${lifeguard.phone})`"
            :value="lifeguard.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit"
        >确认指派</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { editAlertRecord } from "@/api/alertRecordController";
import { listLifeguardVoByPage } from "@/api/lifeguardController";
import { unwrapApiData } from "@/services/serviceUtils";
import type { AlarmRecord } from "@/types/business";

interface FormModel {
  lifeguardId: number | null;
}

interface Props {
  modelValue: boolean;
  alarmInfo?: AlarmRecord | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const loading = ref(false);
const loadingLifeguards = ref(false);
const lifeguardList = ref<
  Array<{ id: number; fullName: string; phone: string }>
>([]);

const form = reactive<FormModel>({
  lifeguardId: null,
});

const formRules = {
  lifeguardId: [{ required: true, message: "请选择救生员", trigger: "change" }],
};

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      await loadLifeguards();
    }
  },
);

const loadLifeguards = async () => {
  try {
    loadingLifeguards.value = true;
    const response = await listLifeguardVoByPage({
      current: 1,
      pageSize: 100,
      dutyStatus: "ON_DUTY",
    });
    const pageData = unwrapApiData<API.PageLifeguardVO>(
      response,
      "获取救生员列表失败",
    );

    lifeguardList.value = (pageData?.records || []).map((item) => ({
      id: item.id!,
      fullName: item.fullName || "-",
      phone: item.phone || "-",
    }));
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  } finally {
    loadingLifeguards.value = false;
  }
};

const getAlarmTypeLabel = (type?: string) => {
  if (type === "drowning") return "溺水";
  if (type === "cross_border") return "越界";
  if (type === "over_capacity") return "超员";
  return type || "-";
};

const handleCancel = () => {
  emit("update:modelValue", false);
  formRef.value?.resetFields();
};

const handleSubmit = async () => {
  if (!formRef.value || !props.alarmInfo) return;

  try {
    await formRef.value.validate();

    loading.value = true;
    const response = await editAlertRecord({
      id: props.alarmInfo.dbId!,
      lifeguardId: form.lifeguardId!,
    });

    unwrapApiData<boolean>(response, "指派失败");
    ElMessage.success("指派成功");
    emit("update:modelValue", false);
    emit("success");
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.alarm-info {
  margin-bottom: 16px;
}
</style>
