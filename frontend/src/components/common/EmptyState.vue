<template>
  <div class="empty-state">
    <el-empty :description="description">
      <template #image>
        <slot name="icon">
          <el-icon class="empty-icon"><Box /></el-icon>
        </slot>
      </template>
      <slot></slot>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'

interface Props {
  description?: string
}

withDefaults(defineProps<Props>(), {
  description: '暂无数据'
})
</script>

<style scoped>
.empty-state {
  padding: 48px 0;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #d9d9d9;
}
</style>
