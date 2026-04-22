<template>
  <div class="user-management-view admin-page">
    <div class="admin-page-header">
      <h1>用户管理</h1>
      <p>管理系统账号、角色与可访问范围</p>
    </div>

    <el-card shadow="never" class="filter-card admin-filter-card">
      <el-form :inline="true" :model="filters" label-width="80px">
        <!-- <el-form-item label="管理范围">
          <el-select
            v-model="filters.venue"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="venueLoading"
            :remote-method="handleVenueRemoteSearch"
            style="width: 160px"
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
        </el-form-item> -->
        <el-form-item label="用户角色">
          <el-select v-model="filters.role" clearable style="width: 160px">
            <el-option label="超级管理员" value="super_admin" />
            <el-option label="场馆管理员" value="venue_admin" />
            <el-option label="救生员" value="lifeguard" />
            <el-option label="查看员" value="viewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="filters.status" clearable style="width: 160px">
            <el-option label="启用" value="enabled" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="admin-round-btn" @click="fetchTable"
            >查询</el-button
          >
          <el-button class="admin-round-btn" @click="resetFilters"
            >重置</el-button
          >
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="admin-table-card">
      <template #header>
        <div class="table-header-toolbar">
          <span class="table-header-title">用户列表</span>
          <div class="table-header-actions">
            <el-button
              type="primary"
              class="admin-round-btn"
              @click="handleCreate"
              >新增用户</el-button
            >
            <el-button class="admin-round-btn" @click="handleBatchEnable"
              >批量启用</el-button
            >
            <el-button class="admin-round-btn" @click="handleBatchDisable"
              >批量禁用</el-button
            >
          </div>
        </div>
      </template>
      <PageTable
        :data="tableData"
        :total="pagination.total"
        :current="pagination.current"
        :page-size="pagination.pageSize"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="用户ID" width="110" />
        <el-table-column prop="account" label="账号" min-width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="角色" width="120">
          <template #default="scope">
            {{ roleLabel(scope.row.role) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="managedVenues"
          label="管理场馆"
          min-width="180"
        />
        <el-table-column label="账号状态" width="100">
          <template #default="scope">
            <StatusTag
              :label="scope.row.status === 'enabled' ? '启用' : '禁用'"
              :type="scope.row.status === 'enabled' ? 'success' : 'default'"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <el-button type="primary" link @click="handleEdit(scope.row)"
                >编辑</el-button
              >
              <el-button
                :type="scope.row.status === 'enabled' ? 'danger' : 'success'"
                link
                @click="handleToggleStatus(scope.row)"
              >
                {{ scope.row.status === "enabled" ? "禁用" : "启用" }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </PageTable>
    </el-card>

    <el-card shadow="never" class="admin-table-card role-card">
      <template #header>
        <div class="role-card__header">
          <span>角色权限配置</span>
          <el-space>
            <el-button type="primary" @click="handleGrantRole"
              >赋予选中用户</el-button
            >
            <el-button @click="handleResetRole">恢复查看员</el-button>
          </el-space>
        </div>
      </template>
      <div class="role-card__content">
        <div class="role-column">
          <div class="role-column__title">角色权限配置</div>
          <el-tree
            :data="roleTree"
            show-checkbox
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleRoleNodeClick"
            @check-change="handleRoleCheckChange"
          />
        </div>
        <div class="role-column">
          <div class="role-column__title">说明</div>
          <p>用于后续承接“角色复制 / 删除 / 树形勾选菜单权限”能力。</p>
        </div>
      </div>
    </el-card>

    <UserAddDialog v-model="addDialogVisible" @success="fetchTable" />
    <UserEditDialog
      v-model="editDialogVisible"
      :user-id="currentUserId"
      @success="fetchTable"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import PageTable from "@/components/business/PageTable.vue";
import StatusTag from "@/components/common/StatusTag.vue";
import { getUserPage } from "@/services/userService";
import {
  getCoreRoleItems,
  getRolePermissionTree,
  type CoreRoleItem,
  type CoreRoleKey,
  type RoleTreeNode,
} from "@/services/adminIntegrationService";
import { assignUserRole } from "@/api/accessControlController";
import { updateUser } from "@/api/userController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";
import type { UserRecord } from "@/types/business";
import UserEditDialog from "./dialogs/UserEditDialog.vue";
import UserAddDialog from "./dialogs/UserAddDialog.vue";

const filters = reactive({
  venue: "",
  role: "" as UserRecord["role"] | "",
  status: "" as UserRecord["status"] | "",
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const tableData = ref<UserRecord[]>([]);
const selectedRows = ref<UserRecord[]>([]);
const roleTree = ref<RoleTreeNode[]>([]);
const coreRoles = ref<CoreRoleItem[]>([]);
const selectedRoleKey = ref<CoreRoleKey | "">("");
const {
  venueOptions,
  venueLoading,
  loadNextPage,
  handleVenueRemoteSearch,
  handleVenueVisibleChange,
  handleVenuePopupScroll,
} = useVenueRemoteSelect<string>({
  valueType: "string",
  errorMessage: "获取场馆列表失败",
});

const editDialogVisible = ref(false);
const addDialogVisible = ref(false);
const currentUserId = ref<string | number>("");

const roleLabel = (role: UserRecord["role"]) => {
  if (role === "super_admin") return "超级管理员";
  if (role === "venue_admin") return "场馆管理员";
  if (role === "lifeguard") return "救生员";
  return "查看员";
};

const fetchTable = async () => {
  const page = await getUserPage({
    current: pagination.current,
    pageSize: pagination.pageSize,
    role: filters.role,
    status: filters.status,
  });
  tableData.value = page.list;
  pagination.total = page.total;
};

const fetchRoleTree = async () => {
  coreRoles.value = await getCoreRoleItems();
  roleTree.value = await getRolePermissionTree();
};

const getSelectedCoreRole = () => {
  if (!selectedRoleKey.value) {
    ElMessage.warning("请先在左侧选择一个角色");
    return;
  }
  const role = coreRoles.value.find(
    (item) => item.key === selectedRoleKey.value,
  );
  if (!role) {
    ElMessage.warning("未找到可操作角色，请刷新后重试");
    return;
  }
  return role;
};

const handleRoleNodeClick = (node: RoleTreeNode) => {
  if (node.id === "ADMIN" || node.id === "LIFEGUARD") {
    selectedRoleKey.value = node.id;
  }
};

const handleRoleCheckChange = (node: RoleTreeNode, checked: boolean) => {
  if (node.id !== "ADMIN" && node.id !== "LIFEGUARD") {
    return;
  }
  if (checked) {
    selectedRoleKey.value = node.id;
    return;
  }
  if (selectedRoleKey.value === node.id) {
    selectedRoleKey.value = "";
  }
};

const getSelectedUsers = () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择用户");
    return;
  }
  return selectedRows.value;
};

const handleGrantRole = async () => {
  const users = getSelectedUsers();
  if (!users) {
    return;
  }
  const source = getSelectedCoreRole();
  if (!source) {
    return;
  }
  let successCount = 0;
  for (const user of users) {
    try {
      const response = await assignUserRole({
        userId: Number(user.id),
        roleCodes: [source.roleCode],
      });
      unwrapApiData<boolean>(response, "赋予角色失败");
      successCount++;
    } catch (error) {
      ElMessage.error(`用户 ${user.name} 赋予角色失败`);
    }
  }
  if (successCount > 0) {
    ElMessage.success(`已为 ${successCount} 个用户赋予“${source.label}”角色`);
  }
  await fetchTable();
};

const handleResetRole = async () => {
  const users = getSelectedUsers();
  if (!users) {
    return;
  }
  let successCount = 0;
  for (const user of users) {
    try {
      const response = await assignUserRole({
        userId: Number(user.id),
        roleCodes: ["USER"],
      });
      unwrapApiData<boolean>(response, "恢复查看员失败");
      successCount++;
    } catch (error) {
      ElMessage.error(`用户 ${user.name} 恢复查看员失败`);
    }
  }
  if (successCount > 0) {
    ElMessage.success(`已将 ${successCount} 个用户恢复为查看员`);
  }
  await fetchTable();
};

const resetFilters = () => {
  filters.venue = "";
  filters.role = "";
  filters.status = "";
  pagination.current = 1;
  void fetchTable();
};

const handlePageChange = (current: number) => {
  pagination.current = current;
  void fetchTable();
};

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size;
  pagination.current = 1;
  void fetchTable();
};

const handleCreate = () => {
  addDialogVisible.value = true;
};

const handleEdit = (user: UserRecord) => {
  currentUserId.value = user.id;
  editDialogVisible.value = true;
};

const handleSelectionChange = (rows: UserRecord[]) => {
  selectedRows.value = rows;
};

const handleBatchEnable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择用户");
    return;
  }
  let successCount = 0;
  for (const row of selectedRows.value) {
    try {
      const res = await updateUser({ id: Number(row.id), status: 1 });
      unwrapApiData<boolean>(res, "启用失败");
      successCount++;
    } catch (e) {
      ElMessage.error(`用户 ${row.name} 启用失败`);
    }
  }
  if (successCount > 0) ElMessage.success(`已成功启用 ${successCount} 个用户`);
  await fetchTable();
};

const handleBatchDisable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择用户");
    return;
  }
  let successCount = 0;
  for (const row of selectedRows.value) {
    try {
      const res = await updateUser({ id: Number(row.id), status: 0 });
      unwrapApiData<boolean>(res, "禁用失败");
      successCount++;
    } catch (e) {
      ElMessage.error(`用户 ${row.name} 禁用失败`);
    }
  }
  if (successCount > 0) ElMessage.success(`已成功禁用 ${successCount} 个用户`);
  await fetchTable();
};

const handleToggleStatus = async (user: UserRecord) => {
  const action = user.status === "enabled" ? "禁用" : "启用";
  const newStatus = user.status === "enabled" ? 0 : 1;

  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${user.name}" 吗？`,
      "确认操作",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    const response = await updateUser({
      id: Number(user.id),
      status: newStatus,
    });

    unwrapApiData<boolean>(response, `${action}失败`);
    ElMessage.success(`${action}成功`);
    await fetchTable();
  } catch (error) {
    if (error !== "cancel") {
      const errorMessage =
        error instanceof Error ? error.message : `${action}失败，请稍后重试`;
      ElMessage.error(errorMessage);
    }
  }
};

onMounted(async () => {
  await loadNextPage();
  await fetchTable();
  await fetchRoleTree();
});
</script>

<style scoped>
.user-management-view {
  min-height: 100%;
}

.filter-card {
  border: none;
}

.table-header-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.table-header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.table-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.table-action-group {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.role-card {
  border: 1px solid var(--color-border);
}

.role-card__header,
.role-card__content {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.role-column {
  flex: 1;
}

.role-column__title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
</style>
