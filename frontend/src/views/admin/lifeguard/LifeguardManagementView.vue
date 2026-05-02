<template>
  <div class="lifeguard-management-view admin-page">
    <div class="admin-page-header">
      <h1>救生员管理</h1>
      <p>查看救生员在岗状态与最近定位上报信息</p>
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
            :loading="venuesLoading"
            :remote-method="handleVenueRemoteSearch"
            style="width: 160px"
            @visible-change="handleVenueVisibleChange"
            @popup-scroll="handleVenuePopupScroll"
          >
            <el-option
              v-for="item in venueOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="在岗状态">
          <el-select
            v-model="filters.dutyStatus"
            clearable
            style="width: 160px"
          >
            <el-option label="在岗" value="on_duty" />
            <el-option label="离岗" value="off_duty" />
            <el-option label="围栏外" value="out_of_fence" />
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

    <div class="lifeguard-main-grid">
      <el-card shadow="never" class="admin-table-card map-card">
        <template #header>
          <div class="map-header">
            <span>在岗地图监控</span>
            <el-space>
              <!-- <el-button @click="openMapTrajectory">查看轨迹</el-button> -->
              <el-button @click="openFenceConfig">围栏配置</el-button>
            </el-space>
          </div>
        </template>
        <LifeguardMapView :lifeguards="tableData" />
      </el-card>

      <div class="summary-stack">
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">在岗人数</div>
          <div class="summary-value">{{ onDutyCount }}</div>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">离岗人数</div>
          <div class="summary-value warning">{{ offDutyCount }}</div>
        </el-card>
        <el-card shadow="never" class="summary-card">
          <div class="summary-title">围栏外人数</div>
          <div class="summary-value danger">{{ outFenceCount }}</div>
        </el-card>
      </div>
    </div>

    <el-card shadow="never" class="admin-table-card">
      <template #header>
        <div class="table-header-toolbar">
          <span class="table-header-title">救生员列表</span>
          <div class="table-header-actions">
            <el-button
              type="primary"
              class="admin-round-btn"
              @click="handleCreate"
              >新增救生员</el-button
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
        <el-table-column prop="id" label="救生员ID" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="联系方式" width="140" />
        <el-table-column prop="venue" label="所属场馆" width="100" />
        <el-table-column label="审核状态" width="110">
          <template #default="scope">
            <StatusTag
              :label="getLifeguardAuditStatusMeta(scope.row.auditStatus).label"
              :type="getLifeguardAuditStatusMeta(scope.row.auditStatus).type"
            />
          </template>
        </el-table-column>
        <el-table-column label="在岗状态" width="120">
          <template #default="scope">
            <StatusTag
              :label="getLifeguardStatusMeta(scope.row.dutyStatus).label"
              :type="getLifeguardStatusMeta(scope.row.dutyStatus).type"
              :emphasized="
                getLifeguardStatusMeta(scope.row.dutyStatus).emphasized
              "
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="lastReportTime"
          label="最近定位上报"
          min-width="160"
        />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <el-button type="primary" link @click="handleEdit(scope.row)"
                >编辑</el-button
              >
              <!-- <el-button
                type="primary"
                link
                @click="handleViewTrajectory(scope.row)"
                >查看轨迹</el-button
              > -->
              <el-button
                type="primary"
                link
                @click="handleFenceConfig(scope.row)"
                >围栏配置</el-button
              >
            </div>
          </template>
        </el-table-column>
      </PageTable>
    </el-card>

    <LifeguardAddDialog v-model="addDialogVisible" @success="fetchTable" />
    <LifeguardEditDialog
      v-model="editDialogVisible"
      :lifeguard-id="currentLifeguardId"
      @success="fetchTable"
    />
    <LifeguardTrajectoryDialog
      v-model="trajectoryDialogVisible"
      :lifeguard-id="currentLifeguardId"
    />
    <LifeguardFenceDialog
      v-model="fenceDialogVisible"
      :venue-id="currentVenueId"
      @saved="fetchTable"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import PageTable from "@/components/business/PageTable.vue";
import StatusTag from "@/components/common/StatusTag.vue";
import { getLifeguardPage } from "@/services/lifeguardService";
import type { LifeguardRecord } from "@/types/business";
import {
  getLifeguardStatusMeta,
  getLifeguardAuditStatusMeta,
} from "@/utils/businessFormatters";
import LifeguardEditDialog from "./dialogs/LifeguardEditDialog.vue";
import LifeguardAddDialog from "./dialogs/LifeguardAddDialog.vue";
import LifeguardTrajectoryDialog from "./dialogs/LifeguardTrajectoryDialog.vue";
import LifeguardFenceDialog from "./dialogs/LifeguardFenceDialog.vue";
import LifeguardMapView from "@/components/business/LifeguardMapView.vue";
import { updateLifeguard } from "@/api/lifeguardController";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import { unwrapApiData } from "@/services/serviceUtils";
import { ElMessage } from "element-plus";

const filters = reactive({
  venueId: "",
  dutyStatus: "" as LifeguardRecord["dutyStatus"] | "",
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const tableData = ref<LifeguardRecord[]>([]);
const {
  venueOptions,
  venueLoading: venuesLoading,
  loadNextPage,
  handleVenueRemoteSearch,
  handleVenueVisibleChange,
  handleVenuePopupScroll,
} = useVenueRemoteSelect<string>({
  valueType: "string",
  status: 1,
  errorMessage: "加载场馆列表失败",
});
const selectedRows = ref<LifeguardRecord[]>([]);
const editDialogVisible = ref(false);
const addDialogVisible = ref(false);
const trajectoryDialogVisible = ref(false);
const fenceDialogVisible = ref(false);
const currentLifeguardId = ref("");
const currentVenueId = ref("");

const venueNameById = computed(() => {
  return new Map(
    venueOptions.value.map((item) => [item.value, item.label] as const),
  );
});

const fetchTable = async () => {
  const page = await getLifeguardPage({
    current: pagination.current,
    pageSize: pagination.pageSize,
    venueId: filters.venueId,
    dutyStatus: filters.dutyStatus,
  });
  tableData.value = page.list.map((item) => ({
    ...item,
    venue: venueNameById.value.get(item.venueId) || item.venue,
  }));
  pagination.total = page.total;
};

const resetFilters = () => {
  filters.venueId = "";
  filters.dutyStatus = "";
  pagination.current = 1;
  void fetchTable();
};

const handlePageChange = (current: number) => {
  pagination.current = current;
  void fetchTable();
};

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize;
  pagination.current = 1;
  void fetchTable();
};

const onDutyCount = computed(
  () => tableData.value.filter((item) => item.dutyStatus === "on_duty").length,
);
const offDutyCount = computed(
  () => tableData.value.filter((item) => item.dutyStatus === "off_duty").length,
);
const outFenceCount = computed(
  () =>
    tableData.value.filter((item) => item.dutyStatus === "out_of_fence").length,
);

const resolveTargetVenueId = (): string => {
  if (currentVenueId.value) return currentVenueId.value;
  if (currentLifeguardId.value) {
    const current = tableData.value.find(
      (item) => item.id === currentLifeguardId.value,
    );
    if (current?.venueId) {
      return current.venueId;
    }
  }
  if (selectedRows.value.length === 1) return selectedRows.value[0].venueId;
  if (tableData.value.length > 0) return tableData.value[0].venueId;
  return "";
};

const openFenceConfig = () => {
  const targetVenueId = resolveTargetVenueId();
  if (!targetVenueId) {
    ElMessage.warning("暂无可配置围栏的场馆");
    return;
  }
  currentVenueId.value = targetVenueId;
  fenceDialogVisible.value = true;
};

const handleCreate = () => {
  addDialogVisible.value = true;
};

const handleSelectionChange = (rows: LifeguardRecord[]) => {
  selectedRows.value = rows;
};

const handleBatchEnable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择救生员");
    return;
  }
  let successCount = 0;
  for (const row of selectedRows.value) {
    try {
      const res = await updateLifeguard({
        id: Number(row.id),
        auditStatus: "APPROVED",
      });
      unwrapApiData<boolean>(res, "启用失败");
      successCount++;
    } catch (e) {
      ElMessage.error(`救生员 ${row.name} 启用失败`);
    }
  }
  if (successCount > 0)
    ElMessage.success(`已成功启用 ${successCount} 名救生员`);
  await fetchTable();
};

const handleBatchDisable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先选择救生员");
    return;
  }
  let successCount = 0;
  for (const row of selectedRows.value) {
    try {
      const res = await updateLifeguard({
        id: Number(row.id),
        auditStatus: "REJECTED",
      });
      unwrapApiData<boolean>(res, "禁用失败");
      successCount++;
    } catch (e) {
      ElMessage.error(`救生员 ${row.name} 禁用失败`);
    }
  }
  if (successCount > 0)
    ElMessage.success(`已成功禁用 ${successCount} 名救生员`);
  await fetchTable();
};

const handleEdit = (row: LifeguardRecord) => {
  currentLifeguardId.value = row.id;
  editDialogVisible.value = true;
};

const handleFenceConfig = (row: LifeguardRecord) => {
  currentLifeguardId.value = row.id;
  currentVenueId.value = row.venueId;
  fenceDialogVisible.value = true;
};

onMounted(async () => {
  await loadNextPage();
  await fetchTable();
});
</script>

<style scoped>
.lifeguard-management-view {
  min-height: 100%;
}

.filter-card {
  border: none;
}

.summary-card {
  border: 1px solid var(--color-border);
}

.lifeguard-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) 320px;
  gap: 16px;
  margin-bottom: 16px;
}

.summary-stack {
  display: grid;
  gap: 16px;
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

.summary-title {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.summary-value {
  margin-top: 12px;
  font-size: 26px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.summary-value.warning {
  color: var(--color-warning);
}

.summary-value.danger {
  color: var(--color-danger);
}

.map-card {
  border: 1px solid var(--color-border);
}

.map-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 1200px) {
  .lifeguard-main-grid {
    grid-template-columns: 1fr;
  }

  .summary-stack {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-stack {
    grid-template-columns: 1fr;
  }
}
</style>
