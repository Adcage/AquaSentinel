<template>
  <div class="system-settings-view admin-page">
    <div class="admin-page-header">
      <h1>系统设置</h1>
      <p>配置系统基础参数、告警阈值与日志查看</p>
    </div>

    <div class="settings-layout">
      <el-card shadow="never" class="settings-nav-card admin-table-card">
        <div class="settings-nav">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="settings-nav__item"
            :class="{ 'is-active': activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </el-card>

      <div class="settings-content">
        <el-card
          v-if="activeTab === 'basic'"
          shadow="never"
          class="admin-table-card"
        >
          <template #header>基础设置</template>
          <el-form label-width="120px" class="settings-form">
            <el-form-item label="系统名称">
              <el-input v-model="form.systemName" />
            </el-form-item>
            <el-form-item label="默认拉流格式">
              <el-select v-model="form.streamType" style="width: 100%">
                <el-option label="HTTP-FLV" value="http-flv" />
                <el-option label="RTSP" value="rtsp" />
              </el-select>
            </el-form-item>
            <div class="field-desc base-settings-desc">
              场馆信息维护请前往「场馆管理」标签页。
            </div>
            <el-form-item>
              <el-button
                type="primary"
                :loading="savingBasic"
                @click="handleSaveBasic"
                >保存设置</el-button
              >
            </el-form-item>
          </el-form>
        </el-card>

        <el-card
          v-if="activeTab === 'notice'"
          shadow="never"
          class="admin-table-card"
        >
          <template #header>通知设置</template>
          <el-form label-width="140px" class="settings-form">
            <el-form-item label="脱岗告警阈值">
              <div class="inline-field">
                <el-input-number
                  v-model="form.offDutyThreshold"
                  :min="10"
                  :max="600"
                />
                <span class="unit-text">秒</span>
              </div>
            </el-form-item>
            <div class="field-desc">超过阈值后触发救生员离岗告警。</div>
            <el-form-item label="设备离线阈值">
              <div class="inline-field">
                <el-input-number
                  v-model="form.deviceOfflineThreshold"
                  :min="10"
                  :max="1200"
                />
                <span class="unit-text">秒</span>
              </div>
            </el-form-item>
            <div class="field-desc">设备连续离线达到阈值后推送告警。</div>
            <el-form-item label="溺水持续判定阈值">
              <div class="inline-field">
                <el-input-number
                  v-model="form.drowningAlertThreshold"
                  :min="1"
                  :max="120"
                />
                <span class="unit-text">秒</span>
              </div>
            </el-form-item>
            <div class="field-desc">模型持续判定超过阈值后触发溺水报警。</div>
            <el-form-item>
              <el-button type="primary" :loading="savingNotice" @click="handleSaveNotice"
                >保存通知配置</el-button
              >
            </el-form-item>
          </el-form>
        </el-card>

        <el-card
          v-if="activeTab === 'venue'"
          shadow="never"
          class="admin-table-card"
        >
          <template #header>场馆管理</template>
          <el-form :inline="true" class="log-filter">
            <el-form-item label="场馆名称">
              <el-input
                v-model="venueFilters.keyword"
                placeholder="请输入场馆名称"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleVenueSearch">查询</el-button>
              <el-button @click="handleVenueReset">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="venue-toolbar">
            <el-button
              type="primary"
              data-test="venue-create-button"
              @click="openVenueCreateDialog"
            >
              新增场馆
            </el-button>
          </div>

          <el-table :data="venueRows" border>
            <el-table-column prop="id" label="场馆ID" width="100" />
            <el-table-column prop="venueCode" label="场馆编码" min-width="140" />
            <el-table-column prop="venueName" label="场馆名称" min-width="180" />
            <el-table-column prop="location" label="场馆位置" min-width="220" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="venue-action-group">
                  <el-button
                    link
                    type="primary"
                    :data-test="`venue-edit-button-${row.id}`"
                    @click="openVenueEditDialog(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    :loading="deletingVenueId === row.id"
                    :data-test="`venue-delete-button-${row.id}`"
                    @click="handleVenueDelete(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="log-pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="venuePagination.total"
              :current-page="venuePagination.current"
              :page-size="venuePagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="handleVenuePageChange"
              @size-change="handleVenuePageSizeChange"
            />
          </div>

          <el-dialog
            v-model="venueDialogVisible"
            :title="editingVenueId ? '编辑场馆' : '新增场馆'"
            width="520px"
          >
            <el-form label-width="96px" class="settings-form">
              <el-form-item label="场馆编码" required>
                <el-input
                  v-model="venueForm.venueCode"
                  data-test="venue-code-input"
                  maxlength="32"
                  placeholder="例如 SH-PD-001"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="场馆名称" required>
                <el-input
                  v-model="venueForm.venueName"
                  data-test="venue-name-input"
                  maxlength="40"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="场馆位置">
                <el-input
                  v-model="venueForm.location"
                  data-test="venue-location-input"
                  placeholder="填写文字地址，如：浦东新区XX路88号"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="venueDialogVisible = false">取消</el-button>
              <el-button
                type="primary"
                :loading="venueSubmitting"
                data-test="venue-submit-button"
                @click="handleVenueSubmit"
              >
                保存
              </el-button>
            </template>
          </el-dialog>
        </el-card>

        <el-card
          v-if="activeTab === 'logs'"
          shadow="never"
          class="admin-table-card"
        >
          <template #header>日志管理</template>
          <el-form :inline="true" class="log-filter">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="logFilters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
              />
            </el-form-item>
            <el-form-item label="操作人">
              <el-input
                v-model="logFilters.operator"
                placeholder="请输入操作人"
              />
            </el-form-item>
            <el-form-item label="操作类型">
              <el-select
                v-model="logFilters.category"
                placeholder="请选择类型"
                style="width: 160px"
              >
                <el-option label="登录日志" value="LOGIN" />
                <el-option label="通用操作" value="OP" />
                <el-option label="报警处理" value="ALERT" />
                <el-option label="AI回调" value="AI_CALLBACK" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadLogs()">查询日志</el-button>
              <el-button @click="handleResetLogs">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="logs" border>
            <el-table-column prop="time" label="时间" min-width="180" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="action" label="操作" min-width="180" />
            <el-table-column prop="result" label="结果" width="100" />
          </el-table>
          <div class="log-pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="logPagination.total"
              :current-page="logPagination.current"
              :page-size="logPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="handleLogPageChange"
              @size-change="handleLogPageSizeChange"
            />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { onMounted, reactive, ref } from "vue";
import { useVenueStore } from "@/stores/venueStore";
import { getSystemLogPage } from "@/services/adminIntegrationService";
import { toLocalDateTimeString } from "@/services/serviceUtils";
import {
  createVenue,
  getVenuePage,
  removeVenue,
  type VenueRecord,
  updateVenueInfo,
} from "@/services/venueService";
import {
  getNoticeSettings,
  saveNoticeSettings,
} from "@/services/systemNoticeSettingsService";

const tabs = [
  { key: "basic", label: "基础设置" },
  { key: "notice", label: "通知设置" },
  { key: "venue", label: "场馆管理" },
  { key: "logs", label: "日志管理" },
] as const;

const activeTab = ref<(typeof tabs)[number]["key"]>("basic");
const savingBasic = ref(false);
const savingNotice = ref(false);
const venueSubmitting = ref(false);
const venueDialogVisible = ref(false);
const editingVenueId = ref<number | undefined>();
const deletingVenueId = ref<number | undefined>();
const venueStore = useVenueStore();

const form = reactive({
  systemName: "AI防溺水监测预警系统",
  streamType: "http-flv",
  offDutyThreshold: 60,
  deviceOfflineThreshold: 180,
  drowningAlertThreshold: 3,
});

const venueFilters = reactive({
  keyword: "",
});

const venueRows = ref<VenueRecord[]>([]);
const venuePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const venueForm = reactive({
  venueCode: "",
  venueName: "",
  location: "",
});

const logFilters = reactive({
  dateRange: [] as Array<string | Date>,
  operator: "",
  category: "",
});

const logs = ref<
  { time: string; operator: string; action: string; result: string }[]
>([]);

const logPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
});

const loadNoticeSettings = async () => {
  try {
    const config = await getNoticeSettings();
    form.offDutyThreshold = config.offDutyThreshold;
    form.deviceOfflineThreshold = config.deviceOfflineThreshold;
    form.drowningAlertThreshold = config.drowningAlertThreshold;
  } catch {
    // keep defaults when backend is unavailable
  }
};

const loadBasic = async () => {
  const savedSystemName = localStorage.getItem("system-name");
  if (savedSystemName) {
    form.systemName = savedSystemName;
  }

  const savedStreamType = localStorage.getItem("system-stream-type");
  if (savedStreamType) {
    form.streamType = savedStreamType;
  }
};

const handleSaveBasic = async () => {
  savingBasic.value = true;
  try {
    localStorage.setItem("system-name", form.systemName);
    localStorage.setItem("system-stream-type", form.streamType);
    ElMessage.success("基础设置已保存");
  } catch (error) {
    ElMessage.error((error as Error).message || "保存失败");
  } finally {
    savingBasic.value = false;
  }
};

const loadVenuePage = async (page = venuePagination.current) => {
  const result = await getVenuePage({
    current: page,
    pageSize: venuePagination.pageSize,
    keyword: venueFilters.keyword || undefined,
  });
  venueRows.value = result.list;
  venuePagination.total = result.total;
  venuePagination.current = result.current;
  venuePagination.pageSize = result.pageSize;
};

const handleVenueSearch = () => {
  venuePagination.current = 1;
  void loadVenuePage(1);
};

const handleVenueReset = () => {
  venueFilters.keyword = "";
  venuePagination.current = 1;
  void loadVenuePage(1);
};

const handleVenuePageChange = (page: number) => {
  venuePagination.current = page;
  void loadVenuePage(page);
};

const handleVenuePageSizeChange = (size: number) => {
  venuePagination.pageSize = size;
  venuePagination.current = 1;
  void loadVenuePage(1);
};

const resetVenueForm = () => {
  venueForm.venueCode = "";
  venueForm.venueName = "";
  venueForm.location = "";
};

const openVenueCreateDialog = () => {
  editingVenueId.value = undefined;
  resetVenueForm();
  venueDialogVisible.value = true;
};

const openVenueEditDialog = (row: VenueRecord) => {
  editingVenueId.value = row.id;
  venueForm.venueCode = row.venueCode || "";
  venueForm.venueName = row.venueName;
  venueForm.location = row.location === "-" ? "" : row.location;
  venueDialogVisible.value = true;
};

const handleVenueSubmit = async () => {
  const code = venueForm.venueCode.trim();
  const name = venueForm.venueName.trim();
  if (!code) {
    ElMessage.warning("请输入场馆编码");
    return;
  }
  if (!name) {
    ElMessage.warning("请输入场馆名称");
    return;
  }

  venueSubmitting.value = true;
  try {
    if (editingVenueId.value) {
      await updateVenueInfo(editingVenueId.value, {
        venueCode: code,
        venueName: name,
        location: venueForm.location.trim() || undefined,
      });
      ElMessage.success("场馆信息已更新");
    } else {
      await createVenue({
        venueCode: code,
        venueName: name,
        location: venueForm.location.trim() || undefined,
      });
      ElMessage.success("场馆已新增");
    }
    venueDialogVisible.value = false;
    venueStore.bumpRevision();
    await loadVenuePage(editingVenueId.value ? venuePagination.current : 1);
  } catch (error) {
    ElMessage.error((error as Error).message || "保存场馆失败");
  } finally {
    venueSubmitting.value = false;
  }
};

const handleVenueDelete = async (row: VenueRecord) => {
  if (deletingVenueId.value != null) {
    return;
  }
  deletingVenueId.value = row.id;
  try {
    await removeVenue(row.id);
    ElMessage.success("场馆已删除");
    venueStore.bumpRevision();
    const nextPage =
      venueRows.value.length <= 1 && venuePagination.current > 1
        ? venuePagination.current - 1
        : venuePagination.current;
    await loadVenuePage(nextPage);
  } catch (error) {
    ElMessage.error((error as Error).message || "删除场馆失败");
  } finally {
    deletingVenueId.value = undefined;
  }
};

const handleSaveNotice = async () => {
  savingNotice.value = true;
  try {
    const config = await saveNoticeSettings({
      offDutyThreshold: form.offDutyThreshold,
      deviceOfflineThreshold: form.deviceOfflineThreshold,
      drowningAlertThreshold: form.drowningAlertThreshold,
    });
    form.offDutyThreshold = config.offDutyThreshold;
    form.deviceOfflineThreshold = config.deviceOfflineThreshold;
    form.drowningAlertThreshold = config.drowningAlertThreshold;
    ElMessage.success("通知配置已保存");
  } catch (error) {
    ElMessage.error((error as Error).message || "保存失败");
  } finally {
    savingNotice.value = false;
  }
};

const loadLogs = async (page: number = logPagination.current) => {
  const targetPage = typeof page === "number" ? page : logPagination.current;
  const [startRaw, endRaw] = logFilters.dateRange;
  const startCreatedAt = startRaw ? toLocalDateTimeString(startRaw) : undefined;
  const endCreatedAt = endRaw ? toLocalDateTimeString(endRaw) : undefined;
  const logPage = await getSystemLogPage({
    current: targetPage,
    pageSize: logPagination.pageSize,
    operatorName: logFilters.operator || undefined,
    logCategory: logFilters.category || undefined,
    startCreatedAt,
    endCreatedAt,
  });
  logs.value = logPage.rows;
  logPagination.total = logPage.total;
  logPagination.current = logPage.current;
  logPagination.pageSize = logPage.pageSize;
};

const handleLogPageChange = (page: number) => {
  void loadLogs(page);
};

const handleLogPageSizeChange = (size: number) => {
  logPagination.pageSize = size;
  logPagination.current = 1;
  void loadLogs(1);
};

const handleResetLogs = () => {
  logFilters.dateRange = [];
  logFilters.operator = "";
  logFilters.category = "";
  logPagination.current = 1;
  void loadLogs(1);
};

onMounted(async () => {
  await loadBasic();
  await loadNoticeSettings();
  await loadVenuePage();
  await loadLogs();
});
</script>

<style scoped>
.system-settings-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-layout {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 16px;
}

.settings-nav-card {
  border: 1px solid var(--color-border);
}

.settings-nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 2px;
}

.settings-nav__item {
  position: relative;
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  text-align: left;
  padding: 0 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.settings-nav__item.is-active {
  border-color: rgba(27, 79, 155, 0.18);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(27, 79, 155, 0.08);
}

.settings-nav__item.is-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 10px;
  width: 3px;
  height: 24px;
  border-radius: 0 2px 2px 0;
  background: var(--color-primary);
}

.settings-nav__item:hover {
  color: var(--color-primary-dark);
  border-color: rgba(27, 79, 155, 0.2);
}

.settings-form {
  max-width: 640px;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 12px;
}

.unit-text,
.field-desc {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.field-desc {
  margin: -12px 0 16px 140px;
}

.base-settings-desc {
  margin-bottom: 20px;
}

.venue-toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}

.venue-action-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.log-filter {
  margin-bottom: 12px;
}

.log-pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }

  .settings-nav {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }

  .settings-nav__item {
    flex: 1;
    min-width: 120px;
  }

  .settings-nav__item.is-active::before {
    top: auto;
    left: 12px;
    bottom: 2px;
    width: calc(100% - 24px);
    height: 3px;
    border-radius: 2px;
  }

  .field-desc {
    margin-left: 0;
  }
}
</style>
