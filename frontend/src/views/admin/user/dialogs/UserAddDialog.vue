<template>
  <el-dialog
    :model-value="modelValue"
    title="新增用户"
    width="620px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="130px">
      <el-form-item label="账号" prop="username">
        <el-input v-model="form.username" placeholder="请输入登录账号" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入登录密码"
          show-password
        />
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
      <el-form-item label="强制修改密码">
        <el-switch v-model="forceChangePasswordBool" />
        <span style="margin-left: 12px; color: #909399; font-size: 12px">
          开启后用户首次登录需修改密码
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
import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { addUser } from "@/api/userController";
import { unwrapApiData } from "@/services/serviceUtils";

interface FormModel {
  username: string;
  password: string;
  displayName: string;
  phone: string;
  email: string;
  roleCodes: string[];
  status: number;
  forceChangePassword: number;
}

interface Props {
  modelValue: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive<FormModel>({
  username: "",
  password: "",
  displayName: "",
  phone: "",
  email: "",
  roleCodes: [],
  status: 1,
  forceChangePassword: 0,
});

const forceChangePasswordBool = computed({
  get: () => form.forceChangePassword === 1,
  set: (val: boolean) => {
    form.forceChangePassword = val ? 1 : 0;
  },
});

const formRules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不少于6位", trigger: "blur" },
  ],
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

const resetForm = () => {
  formRef.value?.resetFields();
  form.forceChangePassword = 0;
  form.status = 1;
  form.roleCodes = [];
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
    const response = await addUser({
      username: form.username,
      password: form.password,
      displayName: form.displayName,
      phone: form.phone || undefined,
      email: form.email || undefined,
      roleCodes: form.roleCodes,
      status: form.status,
      forceChangePassword: form.forceChangePassword,
    });

    unwrapApiData<number>(response, "新增用户失败");
    ElMessage.success("新增用户成功");
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
