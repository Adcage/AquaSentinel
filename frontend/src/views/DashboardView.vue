<template>
  <div class="dashboard-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">工作台</h1>
        <p class="page-subtitle">数据概览与系统运行状态</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6" class="dashboard-col">
        <StatCard
          title="总访问量"
          subtitle="指标说明"
          value="126560"
          :chart-data="[10, 20, 15, 30, 25, 40, 35]"
          chart-type="area"
          :daily-value="8846"
        />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="dashboard-col">
        <StatCard
          title="总交易额"
          subtitle="指标说明"
          prefix="￥"
          value="258670"
          :trend="{ week: 12, day: 11 }"
        />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="dashboard-col">
        <StatCard
          title="在线用户"
          subtitle="指标说明"
          value="1856"
          :chart-data="[5, 10, 8, 15, 12, 20, 18]"
          chart-type="bar"
          :progress="78"
        />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6" class="dashboard-col">
        <StatCard
          title="系统活跃度"
          subtitle="指标说明"
          value="95%"
          conversion="68%"
          :trend="{ week: 2, day: 5 }"
        />
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="16" class="dashboard-col">
        <el-card shadow="never" class="chart-card">
          <template #header>近七日访问趋势</template>
          <div class="bar-chart-container">
            <BarChart :data="barChartData" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8" class="dashboard-col">
        <el-card shadow="never" class="chart-card">
          <template #header>访问来源占比</template>
          <PieChart :data="pieChartData" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="24" class="dashboard-col">
        <el-card shadow="never" class="chart-card-large">
          <template #header>数据趋势概览</template>
          <div class="line-chart-container">
            <LineChart :data="lineChartData" color="#52c41a" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="12" class="dashboard-col">
        <el-card shadow="never">
          <template #header>模块接入建议</template>
          <el-steps direction="vertical" :active="1">
            <el-step title="新增业务路由" description="在 src/router/basic.ts 或 src/router/admin.ts 添加页面路由" />
            <el-step title="创建页面组件" description="在 src/views 下按业务域建立页面并复用现有布局" />
            <el-step title="接入 API 与状态" description="在 src/api 和 src/stores 中扩展接口与状态管理" />
          </el-steps>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12" class="dashboard-col">
        <el-card shadow="never">
          <template #header>快捷入口</template>
          <el-row :gutter="16">
            <el-col v-for="link in quickLinks" :key="link.name" :span="12" class="quick-link-col">
              <el-button class="quick-link-btn" @click="router.push(link.path)">{{ link.name }}</el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import StatCard from '@/components/dashboard/StatCard.vue'
import BarChart from '@/components/dashboard/BarChart.vue'
import PieChart from '@/components/dashboard/PieChart.vue'
import LineChart from '@/components/dashboard/LineChart.vue'

const router = useRouter()

const barChartData = [
  { month: '周一', value: 450 },
  { month: '周二', value: 620 },
  { month: '周三', value: 830 },
  { month: '周四', value: 540 },
  { month: '周五', value: 910 },
  { month: '周六', value: 780 },
  { month: '周日', value: 1020 }
]

const pieChartData = [
  { name: '直接访问', value: 4500 },
  { name: '邮件营销', value: 1200 },
  { name: '联盟广告', value: 1800 },
  { name: '视频广告', value: 2100 },
  { name: '搜索引擎', value: 3600 }
]

const lineChartData = [
  { month: '01', value: 120 },
  { month: '02', value: 132 },
  { month: '03', value: 101 },
  { month: '04', value: 134 },
  { month: '05', value: 90 },
  { month: '06', value: 230 },
  { month: '07', value: 210 },
  { month: '08', value: 180 },
  { month: '09', value: 250 },
  { month: '10', value: 320 },
  { month: '11', value: 280 },
  { month: '12', value: 350 }
]

const quickLinks = [
  { name: '前台首页', path: '/' },
  { name: '工具示例', path: '/utils-demo' },
  { name: '日期工具', path: '/date-util-demo' },
  { name: '登录页', path: '/user/login' }
]
</script>

<style scoped>
.dashboard-view {
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: var(--color-text-main);
}

.page-subtitle {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
}

.dashboard-col {
  margin-bottom: 16px;
}

.content-row {
  margin-top: 0;
}

.chart-card {
  min-height: 300px;
}

.chart-card-large {
  min-height: 300px;
}

.bar-chart-container {
  height: 300px;
}

.line-chart-container {
  height: 300px;
}

.quick-link-col {
  margin-bottom: 16px;
}

.quick-link-btn {
  width: 100%;
}
</style>
