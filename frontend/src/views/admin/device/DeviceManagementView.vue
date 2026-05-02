<template>
  <div class="device-management-view admin-page">
    <div class="admin-page-header">
      <h1>设备管理</h1>
      <p>管理摄像头设备配置、状态与维护周期</p>
    </div>

    <el-card shadow="never" class="filter-card admin-filter-card">
      <el-form :inline="true" :model="filters" label-width="80px">
        <el-form-item label="所属场馆">
          <el-select
            v-model="filters.venueId"
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
        </el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="filters.status" clearable style="width: 160px">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="异常" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select
            v-model="filters.deviceType"
            clearable
            style="width: 160px"
          >
            <el-option label="固定枪机" value="fixed" />
            <el-option label="云台" value="ptz" />
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
          <span class="table-header-title">设备列表</span>
          <div class="table-header-actions">
            <el-button
              type="primary"
              class="admin-round-btn"
              @click="handleCreate"
              >新增设备</el-button
            >
            <el-button class="admin-round-btn" @click="handleBatchEnable"
              >批量启用</el-button
            >
            <el-button class="admin-round-btn" @click="handleBatchDisable"
              >批量禁用</el-button
            >
            <el-dropdown @command="handleExportMaintenance">
              <el-button class="admin-round-btn">
                导出维护记录
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="csv">导出 CSV</el-dropdown-item>
                  <el-dropdown-item command="excel"
                    >导出 Excel</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
        <el-table-column prop="id" label="设备ID" width="100" />
        <el-table-column prop="name" label="设备名称" min-width="160" />
        <el-table-column prop="venue" label="所属场馆" width="90" />
        <el-table-column prop="location" label="安装位置" min-width="160" />
        <el-table-column label="设备类型" width="100">
          <template #default="scope">
            {{ scope.row.deviceType === "fixed" ? "固定枪机" : "云台" }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <StatusTag
              :label="getDeviceStatusMeta(scope.row.status).label"
              :type="getDeviceStatusMeta(scope.row.status).type"
              :emphasized="getDeviceStatusMeta(scope.row.status).emphasized"
            />
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.enabled === 1 ? 'success' : 'info'"
              size="small"
            >
              {{ scope.row.enabled === 1 ? "已启用" : "已禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="maintenanceCycleDays"
          label="维护周期(天)"
          width="120"
        />
        <el-table-column prop="streamUrl" label="视频流地址" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <el-button link type="primary" @click="handleEdit(scope.row)"
                >编辑</el-button
              >
              <el-popconfirm
                title="确认删除该设备？"
                @confirm="handleDelete(scope.row)"
              >
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </PageTable>
    </el-card>

    <DeviceFormDialog
      v-model="dialogVisible"
      :device-id="editingDeviceId"
      @success="fetchTable"
    />

    <el-card shadow="never" class="admin-table-card maintenance-card">
      <template #header>
        <div class="maintenance-header">
          <span>维护记录</span>
          <el-button link type="primary">查看全部</el-button>
        </div>
      </template>
      <el-table :data="maintenanceRows" border>
        <el-table-column prop="deviceName" label="设备名称" min-width="160" />
        <el-table-column prop="content" label="维护记录" min-width="220" />
        <el-table-column prop="operator" label="维护人" width="120" />
        <el-table-column prop="time" label="维护时间" min-width="180" />
      </el-table>
      <div class="maintenance-pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="maintenancePagination.total"
          :current-page="maintenancePagination.current"
          :page-size="maintenancePagination.pageSize"
          @current-change="handleMaintenancePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { ArrowDown } from "@element-plus/icons-vue";
import { onMounted, reactive, ref } from "vue";
import DeviceFormDialog from "@/components/business/DeviceFormDialog.vue";
import PageTable from "@/components/business/PageTable.vue";
import StatusTag from "@/components/common/StatusTag.vue";
import {
  getDevicePage,
  removeDevice,
  type DeviceQuery,
} from "@/services/deviceService";
import { getDeviceMaintenancePage } from "@/services/adminIntegrationService";
import {
  batchDisableCameraDevices,
  updateCameraDevice,
} from "@/api/cameraDeviceController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";
import type { DeviceRecord } from "@/types/business";
import { getDeviceStatusMeta } from "@/utils/businessFormatters";
import ExcelUtil from "@/utils/excel";

const filters = reactive({
  venueId: "",
  status: "" as DeviceQuery["status"],
  deviceType: "" as DeviceQuery["deviceType"],
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const tableData = ref<DeviceRecord[]>([]);
const selectedRows = ref<DeviceRecord[]>([]);
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
const maintenanceRows = ref<
  { deviceName: string; content: string; operator: string; time: string }[]
>([]);
const maintenancePagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});
const dialogVisible = ref(false);
const editingDeviceId = ref<number | undefined>(undefined);

const fetchTable = async () => {
  const page = await getDevicePage({
    current: pagination.current,
    pageSize: pagination.pageSize,
    venueId: filters.venueId,
    status: filters.status,
    deviceType: filters.deviceType,
  });
  tableData.value = page.list;
  pagination.total = page.total;
};

const fetchMaintenance = async (page = maintenancePagination.current) => {
  const result = await getDeviceMaintenancePage({
    current: page,
    pageSize: maintenancePagination.pageSize,
  });
  maintenanceRows.value = result.rows;
  maintenancePagination.total = result.total;
  maintenancePagination.current = result.current;
  maintenancePagination.pageSize = result.pageSize;
};

const handleMaintenancePageChange = (current: number) => {
  void fetchMaintenance(current);
};

const resetFilters = () => {
  filters.venueId = "";
  filters.status = "";
  filters.deviceType = "";
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
  editingDeviceId.value = undefined;
  dialogVisible.value = true;
};

const handleEdit = (row: DeviceRecord) => {
  editingDeviceId.value = Number(row.id);
  dialogVisible.value = true;
};

const handleDelete = async (row: DeviceRecord) => {
  await removeDevice(row.id);
  ElMessage.success("删除成功");
  await fetchTable();
};

const handleSelectionChange = (rows: DeviceRecord[]) => {
  selectedRows.value = rows;
};

const handleBatchEnable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择设备");
    return;
  }
  let successCount = 0;
  for (const row of selectedRows.value) {
    try {
      const res = await updateCameraDevice({
        id: Number(row.id),
        enabled: 1,
        deviceStatus: "ONLINE",
      });
      unwrapApiData<boolean>(res, "启用失败");
      successCount++;
    } catch (e) {
      ElMessage.error(`设备 ${row.name} 启用失败`);
    }
  }
  if (successCount > 0) ElMessage.success(`已成功启用 ${successCount} 台设备`);
  await fetchTable();
};

const handleBatchDisable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择设备");
    return;
  }
  const cameraIds = selectedRows.value.map((row) => Number(row.id));
  const res = await batchDisableCameraDevices({ cameraIds });
  const result = unwrapApiData<API.BatchOperateResultVO>(res, "批量禁用失败");
  const successCount = Number(result.successCount ?? 0);
  const failed = result.failed ?? [];
  if (failed.length > 0) {
    const lines = failed
      .slice(0, 3)
      .map((item) => `设备 ${item.id ?? "-"}：${item.reason || "禁用失败"}`);
    const rest = failed.length > 3 ? `；另有 ${failed.length - 3} 台设备失败` : "";
    ElMessage.error(`${lines.join("；")}${rest}`);
  }
  if (successCount > 0) ElMessage.success(`已成功禁用 ${successCount} 台设备`);
  await fetchTable();
};

const handleExportMaintenance = (format: "csv" | "excel") => {
  if (!maintenanceRows.value.length) {
    ElMessage.warning("暂无维护记录可导出");
    return;
  }
  const header = ["设备名称", "维护记录", "维护人", "维护时间"];
  const fileName = `维护记录_${new Date().toLocaleDateString("zh-CN").replace(/\//g, "-")}`;
  const rows = maintenanceRows.value.map((r) => ({
    设备名称: r.deviceName,
    维护记录: r.content,
    维护人: r.operator,
    维护时间: r.time,
  }));
  if (format === "excel") {
    const result = ExcelUtil.exportExcel(rows, fileName, {
      sheetName: "维护记录",
      header,
      autoWidth: true,
    });
    if (!result.success) ElMessage.error("导出 Excel 失败");
  } else {
    const result = ExcelUtil.exportCSV(rows, fileName, { header });
    if (!result.success) ElMessage.error("导出 CSV 失败");
  }
};

onMounted(async () => {
  await loadNextPage();
  await fetchTable();
  await fetchMaintenance();
});
</script>

<style scoped>
.device-management-view {
  min-height: 100%;
}

.filter-card {
  border: none;
}

.toolbar {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.maintenance-card {
  border: 1px solid var(--color-border);
}

.maintenance-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.maintenance-pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
