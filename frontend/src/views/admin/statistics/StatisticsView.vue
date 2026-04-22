<template>
  <div class="statistics-view admin-page">
    <div class="admin-page-header">
      <h1>统计分析</h1>
      <p>按时间与场馆维度查看报警趋势、分布与导出记录</p>
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
          />
        </el-form-item>
        <el-form-item label="场馆">
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
          <el-select v-model="filters.alarmType" clearable style="width: 160px">
            <el-option label="溺水" value="drowning" />
            <el-option label="越界" value="cross_border" />
            <el-option label="超员" value="over_capacity" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="admin-round-btn" @click="reload"
            >查询</el-button
          >
          <el-button class="admin-round-btn" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="kpi-card">
          <el-statistic title="报警总量" :value="kpi.alarmTotal" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="kpi-card">
          <el-statistic
            title="处理完成率"
            :value="kpi.resolvedRate"
            suffix="%"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="kpi-card">
          <el-statistic
            title="平均响应时长"
            :value="kpi.avgResponseSeconds"
            suffix="秒"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="kpi-card">
          <el-statistic title="高风险场馆数" :value="kpi.highRiskVenueCount" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="admin-table-card">
      <template #header>
        <div class="card-header">
          <span>报警趋势统计</span>
          <el-space>
            <span class="chart-label">图表类型</span>
            <el-radio-group size="small" v-model="chartType">
              <el-radio-button value="bar">柱状</el-radio-button>
              <el-radio-button value="line">折线</el-radio-button>
              <el-radio-button value="pie">饼图</el-radio-button>
            </el-radio-group>
          </el-space>
        </div>
      </template>
      <div ref="alarmTrendChartWrapRef" class="chart-wrap">
        <BarChart :data="alarmTrend" />
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="admin-table-card">
          <template #header>报警类型分布</template>
          <PieChart :data="alarmTypeDistribution" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="admin-table-card">
          <template #header>场馆报警排名</template>
          <div class="chart-wrap">
            <BarChart :data="venueRanking" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="admin-table-card">
      <template #header>
        <div class="card-header">
          <span>报表导出记录</span>
          <el-space>
            <el-button :loading="exportLoading.csv" @click="handleExport('csv')"
              >导出CSV</el-button
            >
            <el-button @click="handleExportImage">下载PNG</el-button>
            <el-button
              type="primary"
              :loading="exportLoading.excel"
              @click="handleExport('excel')"
              >导出Excel</el-button
            >
          </el-space>
        </div>
      </template>
      <el-table :data="exportRows" border>
        <el-table-column prop="name" label="报表名称" min-width="220" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="createdAt" label="导出时间" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import { nextTick, onMounted, reactive, ref } from "vue";
import BarChart from "@/components/dashboard/BarChart.vue";
import PieChart from "@/components/dashboard/PieChart.vue";
import { getStoredAuthUser } from "@/services/authService";
import { requestStatsExport } from "@/services/adminIntegrationService";
import { useVenueRemoteSelect } from "@/composables/useVenueRemoteSelect";
import {
  getAlarmTrend,
  getAlarmTypeDistribution,
  getStatisticsKpi,
  getVenueRanking,
} from "@/services/statisticsService";
import { exportRecordStorage } from "@/utils/exportRecordStorage";

const filters = reactive({
  dateRange: [] as string[],
  venueId: "",
  alarmType: "",
});

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

const chartType = ref<"bar" | "line" | "pie">("line");

const kpi = ref({
  alarmTotal: 0,
  resolvedRate: 0,
  avgResponseSeconds: 0,
  highRiskVenueCount: 0,
});

const alarmTrend = ref<{ month: string; value: number }[]>([]);
const venueRanking = ref<{ month: string; value: number }[]>([]);
const alarmTypeDistribution = ref<{ name: string; value: number }[]>([]);
const alarmTrendChartWrapRef = ref<HTMLElement | null>(null);
const exportLoading = reactive({
  csv: false,
  excel: false,
});

const exportRows = ref<
  { name: string; type: string; operator: string; createdAt: string }[]
>(exportRecordStorage.getRecords());

const formatDateParam = (
  date: string | Date | undefined,
): string | undefined => {
  if (!date) return undefined;
  const d = typeof date === "string" ? new Date(date) : date;
  // 格式化为 YYYY-MM-DD，符合后端 LocalDate 解析要求
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const reload = async () => {
  const params = {
    venueId: filters.venueId,
    startDate: formatDateParam(filters.dateRange?.[0]),
    endDate: formatDateParam(filters.dateRange?.[1]),
    alarmType: filters.alarmType || undefined,
  };
  kpi.value = await getStatisticsKpi(params);
  alarmTrend.value = await getAlarmTrend(params);
  venueRanking.value = await getVenueRanking(params);
  alarmTypeDistribution.value = await getAlarmTypeDistribution(params);
};

const reset = () => {
  filters.dateRange = [];
  filters.venueId = "";
  filters.alarmType = "";
  void reload();
};

const buildExportPayload = (): API.StatsExportRequest => {
  return {
    venueId: Number(filters.venueId) || undefined,
    metricType: filters.alarmType || "ALERT",
    startDate: formatDateParam(filters.dateRange?.[0]),
    endDate: formatDateParam(filters.dateRange?.[1]),
  };
};

const handleExport = async (format: "csv" | "excel") => {
  const key = format === "csv" ? "csv" : "excel";
  exportLoading[key] = true;
  try {
    const user = getStoredAuthUser();
    const row = await requestStatsExport(
      format,
      buildExportPayload(),
      user?.displayName || user?.username || "当前用户",
    );
    exportRows.value = [row, ...exportRows.value].slice(0, 20);
    exportRecordStorage.addRecord(row);
    ElMessage.success(`${format.toUpperCase()}已开始下载`);
  } catch (error) {
    ElMessage.error((error as Error).message || "导出失败");
  } finally {
    exportLoading[key] = false;
  }
};

const handleExportImage = async () => {
  await nextTick();
  const chartDom = alarmTrendChartWrapRef.value?.querySelector(
    ".bar-chart, .pie-chart",
  ) as HTMLDivElement | null;
  if (!chartDom) {
    ElMessage.error("图表未渲染完成，无法下载PNG");
    return;
  }

  const chart = echarts.getInstanceByDom(chartDom);
  if (!chart) {
    ElMessage.error("图表实例不可用，无法下载PNG");
    return;
  }

  const pngDataUrl = chart.getDataURL({
    type: "png",
    pixelRatio: 2,
    backgroundColor: "#ffffff",
  });
  const link = document.createElement("a");
  const dateTag = new Date().toISOString().slice(0, 10);
  link.href = pngDataUrl;
  link.download = `报警趋势统计_${dateTag}.png`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  ElMessage.success("PNG已开始下载");
};

onMounted(async () => {
  await loadNextPage();
  await reload();
});
</script>

<style scoped>
.statistics-view {
  min-height: 100%;
}

.filter-card,
.kpi-card {
  border: 1px solid var(--color-border);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.chart-wrap {
  height: 320px;
}
</style>
