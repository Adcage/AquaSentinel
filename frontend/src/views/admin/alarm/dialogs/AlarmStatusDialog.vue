<template>
  <el-dialog
    :model-value="modelValue"
    title="标记状态"
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
        <el-descriptions-item label="当前状态">
          <el-tag :type="getAlarmStatusColor(alarmInfo.status)">
            {{ getAlarmStatusLabel(alarmInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="触发时间">
          {{ alarmInfo.triggerTime }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <el-divider />

    <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
      <el-form-item label="处理状态" prop="alertStatus">
        <el-radio-group v-model="form.alertStatus">
          <el-radio value="PENDING">未处理</el-radio>
          <el-radio value="PROCESSING">处理中</el-radio>
          <el-radio value="RESOLVED">已处理</el-radio>
          <el-radio value="FALSE_ALARM">误报</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit"
        >确认</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { editAlertRecord } from "@/api/alertRecordController";
import { unwrapApiData } from "@/services/serviceUtils";
import type { AlarmRecord } from "@/types/business";

interface FormModel {
  alertStatus: string;
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

const form = reactive<FormModel>({
  alertStatus: "PENDING",
});

const formRules = {
  alertStatus: [
    { required: true, message: "请选择处理状态", trigger: "change" },
  ],
};

watch(
  () => props.alarmInfo,
  (info) => {
    if (info) {
      const statusMap: Record<string, string> = {
        pending: "PENDING",
        processing: "PROCESSING",
        resolved: "RESOLVED",
        false_alarm: "FALSE_ALARM",
      };
      form.alertStatus = statusMap[info.status] || "PENDING";
    }
  },
  { immediate: true },
);

const getAlarmTypeLabel = (type?: string) => {
  if (type === "drowning") return "溺水";
  if (type === "cross_border") return "越界";
  if (type === "over_capacity") return "超员";
  return type || "-";
};

const getAlarmStatusLabel = (status?: string) => {
  if (status === "pending") return "未处理";
  if (status === "processing") return "处理中";
  if (status === "resolved") return "已处理";
  if (status === "false_alarm") return "误报";
  return status || "-";
};

const getAlarmStatusColor = (status?: string) => {
  if (status === "pending") return "danger";
  if (status === "processing") return "warning";
  if (status === "resolved") return "success";
  if (status === "false_alarm") return "info";
  return "";
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
      alertStatus: form.alertStatus,
    });

    unwrapApiData<boolean>(response, "更新状态失败");
    ElMessage.success("更新状态成功");
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
