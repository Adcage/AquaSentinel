<template>
  <el-dialog
    :model-value="modelValue"
    title="新增救生员"
    width="620px"
    :close-on-click-modal="false"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="130px">
      <el-form-item label="账号来源" prop="accountMode">
        <el-radio-group v-model="form.accountMode">
          <el-radio value="bind_existing">关联已有用户</el-radio>
          <el-radio value="create_new">创建新账号</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="姓名" prop="fullName">
        <el-input v-model="form.fullName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item
        v-if="form.accountMode === 'bind_existing'"
        label="关联用户"
        prop="linkedUserId"
      >
        <el-select
          v-model="form.linkedUserId"
          filterable
          remote
          clearable
          reserve-keyword
          :loading="linkableUsersLoading"
          :remote-method="handleLinkableUserSearch"
          placeholder="请输入姓名/登录账号/手机号搜索"
          no-match-text="未找到匹配用户，请尝试姓名、账号或手机号"
          no-data-text="暂无可关联用户"
          style="width: 100%"
          @visible-change="handleLinkableUserVisibleChange"
        >
          <el-option
            v-for="option in linkableUserOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="form.accountMode === 'create_new'"
        label="登录账号"
        prop="username"
      >
        <el-input v-model="form.username" placeholder="请输入登录账号" />
      </el-form-item>
      <el-form-item
        v-if="form.accountMode === 'create_new'"
        label="登录密码"
        prop="password"
      >
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入登录密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱（可选）" />
      </el-form-item>
      <el-form-item label="救生员编码" prop="lifeguardCode">
        <el-input
          v-model="form.lifeguardCode"
          placeholder="可选，留空则自动生成"
        />
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
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { addLifeguard } from "@/api/lifeguardController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";
import { listLinkableLifeguardUsers } from "@/services/userService";

type AccountMode = "bind_existing" | "create_new";

interface FormModel {
  accountMode: AccountMode;
  linkedUserId?: number;
  fullName: string;
  phone: string;
  username: string;
  password: string;
  email: string;
  lifeguardCode: string;
  venueId: number;
  auditStatus: string;
  dutyStatus: string;
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
const {
  venueOptions,
  venueLoading,
  loadNextPage,
  handleVenueRemoteSearch,
  handleVenueVisibleChange,
  handleVenuePopupScroll,
} = useVenueRemoteSelect<number>({
  valueType: "number",
  errorMessage: "获取场馆列表失败",
});

const form = reactive<FormModel>({
  accountMode: "bind_existing",
  linkedUserId: undefined,
  fullName: "",
  phone: "",
  username: "",
  password: "",
  email: "",
  lifeguardCode: "",
  venueId: 0,
  auditStatus: "PENDING",
  dutyStatus: "OFF_DUTY",
});

const linkableUsersLoading = ref(false);
const linkableUserOptions = ref<
  Array<{ value: number; label: string; username: string; phone?: string }>
>([]);

const loadLinkableUsers = async (keyword = "") => {
  linkableUsersLoading.value = true;
  try {
    linkableUserOptions.value = await listLinkableLifeguardUsers(keyword);
  } finally {
    linkableUsersLoading.value = false;
  }
};

const handleLinkableUserSearch = (keyword: string) => {
  void loadLinkableUsers(keyword);
};

const handleLinkableUserVisibleChange = (visible: boolean) => {
  if (!visible) return;
  if (linkableUserOptions.value.length > 0) return;
  void loadLinkableUsers();
};

const formRules = {
  accountMode: [{ required: true, message: "请选择账号来源", trigger: "change" }],
  linkedUserId: [
    {
      validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
        if (form.accountMode !== "bind_existing") {
          callback();
          return;
        }
        if (!value || value <= 0) {
          callback(new Error("请选择要关联的用户"));
          return;
        }
        callback();
      },
      trigger: "change",
    },
  ],
  fullName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号",
      trigger: "blur",
    },
  ],
  username: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (form.accountMode !== "create_new") {
          callback();
          return;
        }
        if (!value || !value.trim()) {
          callback(new Error("请输入登录账号"));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  password: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (form.accountMode !== "create_new") {
          callback();
          return;
        }
        if (!value) {
          callback(new Error("请输入登录密码"));
          return;
        }
        if (value.length < 6) {
          callback(new Error("密码长度不少于6位"));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: "blur" }],
  venueId: [{ required: true, message: "请选择所属场馆", trigger: "change" }],
  auditStatus: [
    { required: true, message: "请选择审核状态", trigger: "change" },
  ],
  dutyStatus: [
    { required: true, message: "请选择在岗状态", trigger: "change" },
  ],
};

const resetForm = () => {
  formRef.value?.resetFields();
  form.accountMode = "bind_existing";
  form.linkedUserId = undefined;
  form.username = "";
  form.password = "";
  form.venueId = venueOptions.value[0]?.value ?? 0;
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
    const payload: API.LifeguardAddRequest = {
      fullName: form.fullName,
      phone: form.phone,
      email: form.email || undefined,
      lifeguardCode: form.lifeguardCode || undefined,
      venueId: form.venueId,
      auditStatus: form.auditStatus,
      dutyStatus: form.dutyStatus,
    };
    if (form.accountMode === "bind_existing") {
      payload.userId = form.linkedUserId;
    } else {
      payload.username = form.username.trim();
      payload.password = form.password;
    }
    const response = await addLifeguard({
      ...payload,
    });

    unwrapApiData<number>(response, "新增救生员失败");
    ElMessage.success("新增救生员成功");
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

onMounted(async () => {
  await loadNextPage();
  await loadLinkableUsers();
  if (form.venueId <= 0) {
    form.venueId = venueOptions.value[0]?.value ?? 0;
  }
});
</script>
