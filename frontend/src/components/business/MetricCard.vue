<template>
  <el-card shadow="never" class="metric-card" :class="{ 'is-danger': danger }">
    <div class="metric-header">
      <el-icon class="metric-icon"><component :is="icon" /></el-icon>
      <span class="metric-title">{{ title }}</span>
    </div>
    <div class="metric-value">{{ value }}</div>
    <div class="metric-footer" v-if="footer">{{ footer }}</div>
  </el-card>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  icon: Component
  title: string
  value: string | number
  footer?: string
  danger?: boolean
}

defineProps<Props>()
</script>

<style scoped>
.metric-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}

.metric-card.is-danger {
  border-left: 4px solid var(--color-danger);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.metric-icon {
  color: var(--color-primary);
}

.metric-value {
  margin-top: 16px;
  font-size: 32px;
  line-height: 1;
  font-weight: 600;
  color: var(--color-text-primary);
}

.metric-footer {
  margin-top: 10px;
  color: var(--color-text-tertiary);
  font-size: 12px;
}
</style>
