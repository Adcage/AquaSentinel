<template>
  <div class="home-view">
    <section class="hero-section">
      <h1>Vue3 + Element Plus 前端模板</h1>
      <p>保留前台基础布局、登录注册流程与后台模板结构，便于二次开发。</p>
      <el-space :size="12" wrap>
        <el-button type="primary" @click="router.push('/user/login')">登录</el-button>
        <el-button @click="router.push('/user/register')">注册</el-button>
        <el-button plain @click="router.push('/admin/dashboard')">进入后台模板</el-button>
      </el-space>
    </section>

    <el-row :gutter="16" class="feature-grid">
      <el-col :xs="24" :md="8" class="feature-col">
        <el-card class="feature-card" shadow="never">
          <template #header>基础路由模板</template>
          <p>内置首页、登录、注册和 404 页面，开箱可用。</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8" class="feature-col">
        <el-card class="feature-card" shadow="never">
          <template #header>双布局结构</template>
          <p>保留前台 BasicLayout 与后台 BackendLayout，便于扩展。</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8" class="feature-col">
        <el-card class="feature-card" shadow="never">
          <template #header>业务解耦</template>
          <p>已移除原有业务页面与组件，减少耦合和维护成本。</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.home-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 20px;
}

.hero-section {
  background: #ffffff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 32px;
  margin-bottom: 24px;
}

.hero-section h1 {
  margin: 0 0 12px;
  font-size: 30px;
  color: var(--color-text-main);
}

.hero-section p {
  margin: 0 0 20px;
  color: var(--color-text-secondary);
  font-size: 15px;
}

.feature-grid {
  margin-top: 8px;
}

.feature-col {
  margin-bottom: 16px;
}

.feature-card {
  height: 100%;
  border: 1px solid var(--color-border);
}

.feature-card p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}
</style>
