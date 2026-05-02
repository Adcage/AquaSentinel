<template>
  <div class="alarm-management-view admin-page">
    <div class="admin-page-header">
      <h1>报警管理</h1>
      <p>按类型、状态和关键词筛选报警记录，支持批量处理</p>
    </div>

    <el-card shadow="never" class="filter-card admin-filter-card">
      <el-form :inline="true" :model="filters" label-width="80px">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
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
        <el-form-item label="报警类型">
          <el-select v-model="filters.type" clearable style="width: 160px">
            <el-option label="溺水" value="drowning" />
            <el-option label="越界" value="cross_border" />
            <el-option label="超员" value="over_capacity" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="filters.status" clearable style="width: 160px">
            <el-option label="未处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已处理" value="resolved" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="报警ID/摄像头位置"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="round-btn admin-round-btn"
            @click="fetchTable"
            >查询</el-button
          >
          <el-button class="round-btn admin-round-btn" @click="resetFilters"
            >重置</el-button
          >
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="admin-table-card">
      <template #header>
        <div class="table-header-toolbar">
          <span class="table-header-title">报警记录</span>
          <div class="table-header-actions">
            <el-button
              type="primary"
              class="batch-action-btn admin-round-btn"
              :disabled="!selection.length"
              @click="handleBatchResolve"
              >批量标记已处理</el-button
            >
            <el-dropdown @command="handleExportRecords">
              <el-button class="admin-round-btn">
                导出记录
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
        :row-class-name="tableRowClassName"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="报警ID" min-width="160" />
        <el-table-column label="报警类型" width="120">
          <template #default="scope">
            <StatusTag
              :label="getAlarmTypeMeta(scope.row.type).label"
              :type="getAlarmTypeMeta(scope.row.type).type"
            />
          </template>
        </el-table-column>
        <el-table-column prop="triggerTime" label="触发时间" min-width="160" />
        <el-table-column
          prop="cameraLocation"
          label="摄像头位置"
          min-width="140"
        />
        <el-table-column
          prop="emergencyContact"
          label="紧急联系人"
          min-width="150"
        />
        <el-table-column prop="lifeguardName" label="处理救生员" width="110" />
        <el-table-column label="处理状态" width="110">
          <template #default="scope">
            <StatusTag
              :label="getAlarmStatusMeta(scope.row.status).label"
              :type="getAlarmStatusMeta(scope.row.status).type"
              :emphasized="getAlarmStatusMeta(scope.row.status).emphasized"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <el-button
                type="primary"
                link
                @click="handleViewDetail(scope.row)"
                >查看详情</el-button
              >
              <el-button type="primary" link @click="handleAssign(scope.row)"
                >指派处理</el-button
              >
              <el-button
                type="primary"
                link
                @click="handleMarkStatus(scope.row)"
                >标记状态</el-button
              >
            </div>
          </template>
        </el-table-column>
      </PageTable>
    </el-card>

    <AlarmDetailDialog
      v-model="detailDialogVisible"
      :alarm-id="currentAlarmId"
    />
    <AlarmAssignDialog
      v-model="assignDialogVisible"
      :alarm-info="currentAlarm"
      @success="fetchTable"
    />
    <AlarmStatusDialog
      v-model="statusDialogVisible"
      :alarm-info="currentAlarm"
      @success="fetchTable"
    />
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { ArrowDown } from "@element-plus/icons-vue";
import { onMounted, reactive, ref } from "vue";
import PageTable from "@/components/business/PageTable.vue";
import StatusTag from "@/components/common/StatusTag.vue";
import { getAlarmPage, markAlarmsResolved } from "@/services/alarmService";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import type { AlarmRecord } from "@/types/business";
import {
  getAlarmStatusMeta,
  getAlarmTypeMeta,
} from "@/utils/businessFormatters";
import AlarmDetailDialog from "./dialogs/AlarmDetailDialog.vue";
import AlarmAssignDialog from "./dialogs/AlarmAssignDialog.vue";
import AlarmStatusDialog from "./dialogs/AlarmStatusDialog.vue";
import ExcelUtil from "@/utils/excel";

const filters = reactive({
  dateRange: [] as string[],
  venueId: "",
  type: "" as AlarmRecord["type"] | "",
  status: "" as AlarmRecord["status"] | "",
  keyword: "",
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const tableData = ref<AlarmRecord[]>([]);
const selection = ref<AlarmRecord[]>([]);
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
const detailDialogVisible = ref(false);
const assignDialogVisible = ref(false);
const statusDialogVisible = ref(false);
const currentAlarmId = ref("");
const currentAlarm = ref<AlarmRecord | null>(null);

const fetchTable = async () => {
  const page = await getAlarmPage({
    current: pagination.current,
    pageSize: pagination.pageSize,
    venueId: filters.venueId,
    keyword: filters.keyword,
    status: filters.status,
    type: filters.type,
  });
  tableData.value = page.list;
  pagination.total = page.total;
};

const resetFilters = () => {
  filters.type = "";
  filters.status = "";
  filters.keyword = "";
  filters.dateRange = [];
  filters.venueId = "";
  pagination.current = 1;
  void fetchTable();
};

const handleBatchResolve = async () => {
  if (!selection.value.length) {
    ElMessage.warning("请先选择报警记录");
    return;
  }
  const records = selection.value;
  try {
    const result = await markAlarmsResolved(records);
    if (result.failedCount > 0 && result.successCount > 0) {
      ElMessage.warning(
        `已处理 ${result.successCount} 条，${result.failedCount} 条失败`,
      );
    } else if (result.failedCount > 0) {
      ElMessage.error(`批量处理失败，共 ${result.failedCount} 条失败`);
    } else {
      ElMessage.success(`已提交批量处理，共 ${result.successCount} 条记录`);
    }
    await fetchTable();
  } catch (e) {
    ElMessage.error(
      e instanceof Error ? e.message : "批量处理失败，请稍后重试",
    );
  }
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

const tableRowClassName = ({ row }: { row: AlarmRecord }) =>
  row.status === "pending" ? "alarm-pending-row" : "";

const handleSelectionChange = (rows: AlarmRecord[]) => {
  selection.value = rows;
};

const handleViewDetail = (row: AlarmRecord) => {
  currentAlarmId.value = String(row.dbId ?? "");
  detailDialogVisible.value = true;
};

const handleAssign = (row: AlarmRecord) => {
  currentAlarm.value = row;
  assignDialogVisible.value = true;
};

const handleMarkStatus = (row: AlarmRecord) => {
  currentAlarm.value = row;
  statusDialogVisible.value = true;
};

const handleExportRecords = (format: "csv" | "excel") => {
  if (!tableData.value.length) {
    ElMessage.warning("暂无报警记录可导出");
    return;
  }
  const exportData = selection.value.length ? selection.value : tableData.value;
  const header = [
    "报警ID",
    "报警类型",
    "触发时间",
    "摄像头位置",
    "紧急联系人",
    "处理救生员",
    "处理状态",
  ];
  const typeLabel = (t: string) =>
    ({ drowning: "溺水", cross_border: "越界", over_capacity: "超员" })[t] || t;
  const statusLabel = (s: string) =>
    ({
      pending: "未处理",
      processing: "处理中",
      resolved: "已处理",
      false_alarm: "误报",
    })[s] || s;
  const fileName = `报警记录_${new Date().toLocaleDateString("zh-CN").replace(/\//g, "-")}`;
  const rows = exportData.map((r) => ({
    报警ID: r.id,
    报警类型: typeLabel(r.type),
    触发时间: r.triggerTime,
    摄像头位置: r.cameraLocation,
    紧急联系人: r.emergencyContact,
    处理救生员: r.lifeguardName,
    处理状态: statusLabel(r.status),
  }));
  if (format === "excel") {
    const result = ExcelUtil.exportExcel(rows, fileName, {
      sheetName: "报警记录",
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
});
</script>

<style scoped>
.alarm-management-view {
  min-height: 100%;
}

.filter-card {
  border: none;
}

.toolbar {
  margin-top: 8px;
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

.round-btn {
  border-radius: 18px;
}

.batch-action-btn {
  border-radius: 18px;
}

:deep(.batch-action-btn.el-button--primary:not(.is-disabled)) {
  background: #2f75b6;
  border-color: #2f75b6;
}

:deep(.batch-action-btn.el-button--primary.is-disabled) {
  background: #b9d8f5;
  border-color: #b9d8f5;
  color: #ffffff;
}

:deep(.alarm-pending-row) {
  color: var(--color-danger);
  font-weight: 600;
  background: #fff1f0;
}

:deep(.alarm-pending-row .el-link),
:deep(.alarm-pending-row .el-button--text) {
  color: var(--color-danger);
}
</style>
