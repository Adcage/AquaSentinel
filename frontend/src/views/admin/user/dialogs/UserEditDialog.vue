<template>
  <el-dialog
    :model-value="modelValue"
    title="编辑用户"
    width="620px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="130px">
      <el-form-item label="用户ID">
        <el-input v-model="form.id" disabled />
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      <el-form-item label="姓名" prop="displayName">
        <el-input v-model="form.displayName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="角色" prop="roleCodes">
        <el-select v-model="form.roleCodes" multiple style="width: 100%">
          <el-option label="超级管理员" value="SUPER_ADMIN" />
          <el-option label="场馆管理员" value="VENUE_ADMIN" />
          <el-option label="救生员" value="LIFEGUARD" />
          <el-option label="查看员" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item label="账号状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="重置密码">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="留空则不修改密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="强制修改密码">
        <el-switch v-model="forceChangePasswordBool" />
        <span style="margin-left: 12px; color: #909399; font-size: 12px">
          开启后用户下次登录需修改密码
        </span>
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
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { getUserVoById, updateUser } from "@/api/userController";
import { unwrapApiData } from "@/services/serviceUtils";

interface FormModel {
  id: string;
  username: string;
  displayName: string;
  phone: string;
  email: string;
  roleCodes: string[];
  status: number;
  password: string;
  forceChangePassword: number;
}

interface Props {
  modelValue: boolean;
  userId?: string | number;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive<FormModel>({
  id: "",
  username: "",
  displayName: "",
  phone: "",
  email: "",
  roleCodes: [],
  status: 1,
  password: "",
  forceChangePassword: 0,
});

const forceChangePasswordBool = computed({
  get: () => form.forceChangePassword === 1,
  set: (val: boolean) => {
    form.forceChangePassword = val ? 1 : 0;
  },
});

const formRules = {
  displayName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号",
      trigger: "blur",
    },
  ],
  email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: "blur" }],
  roleCodes: [{ required: true, message: "请选择角色", trigger: "change" }],
  status: [{ required: true, message: "请选择账号状态", trigger: "change" }],
};

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && props.userId != null) {
      await loadUserData();
    }
  },
);

const loadUserData = async () => {
  if (props.userId == null) return;

  try {
    loading.value = true;
    const response = await getUserVoById({ id: Number(props.userId) });
    const data = unwrapApiData<API.UserVO>(response, "获取用户详情失败");

    form.id = String(data.id ?? "");
    form.username = data.username || "";
    form.displayName = data.displayName || "";
    form.phone = data.phone || "";
    form.email = data.email || "";
    form.roleCodes = data.roleCodes || [];
    form.status = data.status ?? 1;
    form.password = "";
    form.forceChangePassword = data.forceChangePassword ?? 0;
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
};

const handleSubmit = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    const updateData: API.UserUpdateRequest = {
      id: Number(form.id),
      displayName: form.displayName,
      phone: form.phone || undefined,
      email: form.email || undefined,
      status: form.status,
      roleCodes: form.roleCodes,
      forceChangePassword: form.forceChangePassword,
    };

    if (form.password) {
      updateData.password = form.password;
    }

    loading.value = true;
    const response = await updateUser(updateData);
    unwrapApiData<boolean>(response, "更新用户失败");
    ElMessage.success("更新用户成功");
    emit("update:modelValue", false);
    emit("success");
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
