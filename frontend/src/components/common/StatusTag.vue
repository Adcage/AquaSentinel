<template>
  <el-tag
    :type="tagType"
    :color="tagColor"
    :class="['status-tag', { 'is-emphasized': emphasized }]"
    effect="light"
  >
    <slot>{{ label }}</slot>
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  type?: 'success' | 'warning' | 'danger' | 'processing' | 'default'
  label?: string
  color?: string
  emphasized?: boolean
}

const props = defineProps<Props>()

const tagType = computed(() => {
  if (props.color) {
    return undefined
  }

  if (props.type === 'processing') {
    return 'primary'
  }

  if (props.type === 'default' || !props.type) {
    return 'info'
  }

  return props.type
})

const tagColor = computed(() => props.color || undefined)

const emphasized = computed(() => Boolean(props.emphasized))
</script>

<style scoped>
.status-tag {
  border-radius: 2px;
  font-weight: 500;
  padding: 0 8px;
}

.status-tag.is-emphasized {
  font-weight: 600;
}
</style>
