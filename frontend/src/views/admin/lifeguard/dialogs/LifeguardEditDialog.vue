<template>
  <el-dialog
    :model-value="modelValue"
    title="编辑救生员"
    width="600px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
      <el-form-item label="救生员ID">
        <el-input v-model="form.id" disabled />
      </el-form-item>
      <el-form-item label="关联用户ID">
        <el-input
          v-model="form.userId"
          disabled
          placeholder="关联的系统用户ID"
        />
      </el-form-item>
      <el-form-item label="救生员编码">
        <el-input v-model="form.lifeguardCode" placeholder="可选" />
      </el-form-item>
      <el-form-item label="姓名" prop="fullName">
        <el-input v-model="form.fullName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="所属场馆" prop="venueId">
        <el-select
          v-model="form.venueId"
          filterable
          remote
          reserve-keyword
          :loading="venueLoading"
          :remote-method="handleVenueRemoteSearch"
          style="width: 100%"
          @visible-change="handleVenueVisibleChange"
          @popup-scroll="handleVenuePopupScroll"
        >
          <el-option
            v-for="option in venueOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStatus">
        <el-radio-group v-model="form.auditStatus">
          <el-radio value="PENDING">待审核</el-radio>
          <el-radio value="APPROVED">已通过</el-radio>
          <el-radio value="REJECTED">已拒绝</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="在岗状态" prop="dutyStatus">
        <el-radio-group v-model="form.dutyStatus">
          <el-radio value="ON_DUTY">在岗</el-radio>
          <el-radio value="OFF_DUTY">离岗</el-radio>
          <el-radio value="OUT_OF_FENCE">围栏外</el-radio>
        </el-radio-group>
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
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { getLifeguardVoById, updateLifeguard } from "@/api/lifeguardController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";

interface FormModel {
  id: string;
  userId: string;
  lifeguardCode: string;
  fullName: string;
  phone: string;
  venueId: number;
  auditStatus: string;
  dutyStatus: string;
}

interface Props {
  modelValue: boolean;
  lifeguardId?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const loading = ref(false);
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

const form = reactive<FormModel>({
  id: "",
  userId: "",
  lifeguardCode: "",
  fullName: "",
  phone: "",
  venueId: 0,
  auditStatus: "PENDING",
  dutyStatus: "OFF_DUTY",
});

const formRules = {
  fullName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号",
      trigger: "blur",
    },
  ],
  venueId: [{ required: true, message: "请选择所属场馆", trigger: "change" }],
  auditStatus: [
    { required: true, message: "请选择审核状态", trigger: "change" },
  ],
  dutyStatus: [
    { required: true, message: "请选择在岗状态", trigger: "change" },
  ],
};

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && props.lifeguardId) {
      await loadLifeguardData();
    }
  },
);

const loadLifeguardData = async () => {
  if (!props.lifeguardId) return;

  try {
    loading.value = true;
    const response = await getLifeguardVoById({
      id: Number(props.lifeguardId),
    });
    const data = unwrapApiData<API.LifeguardVO>(response, "获取救生员详情失败");
    const normalizedVenueId = Number(data.venueId ?? 0);
    await ensureVenueOption(normalizedVenueId);

    form.id = String(data.id ?? "");
    form.userId = String(data.userId ?? "");
    form.lifeguardCode = data.lifeguardCode || "";
    form.fullName = data.fullName || "";
    form.phone = data.phone || "";
    form.venueId = normalizedVenueId || venueOptions.value[0]?.value || 0;
    form.auditStatus = data.auditStatus || "PENDING";
    form.dutyStatus = data.dutyStatus || "OFF_DUTY";
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  } finally {
    loading.value = false;
  }
};

const handleCancel = () => {
  emit("update:modelValue", false);
  formRef.value?.resetFields();
  form.venueId = venueOptions.value[0]?.value ?? 0;
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    loading.value = true;
    const response = await updateLifeguard({
      id: Number(form.id),
      userId: form.userId ? Number(form.userId) : undefined,
      lifeguardCode: form.lifeguardCode || undefined,
      fullName: form.fullName,
      phone: form.phone,
      venueId: form.venueId,
      auditStatus: form.auditStatus,
      dutyStatus: form.dutyStatus,
    });

    unwrapApiData<boolean>(response, "更新救生员失败");
    ElMessage.success("更新救生员成功");
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

onMounted(async () => {
  await loadNextPage();
  if (form.venueId <= 0) {
    form.venueId = venueOptions.value[0]?.value ?? 0;
  }
});
</script>
