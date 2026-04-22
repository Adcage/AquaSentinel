<template>
  <div class="page-table">
    <el-table
      v-bind="tableProps"
      border
      :data="data"
      :row-class-name="rowClassName"
      @selection-change="handleSelectionChange"
    >
      <slot />
    </el-table>
    <div class="pagination-wrap">
      <el-pagination
        background
        layout="->, total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="current"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends Record<string, unknown>">
import type { TableProps } from 'element-plus'

interface Props {
  data: T[]
  total: number
  current: number
  pageSize: number
  tableProps?: Partial<TableProps<T>>
  rowClassName?: TableProps<T>['rowClassName']
}

const props = defineProps<Props>()

const emit = defineEmits<{
  pageChange: [current: number]
  pageSizeChange: [pageSize: number]
  selectionChange: [rows: T[]]
}>()

const handleCurrentChange = (current: number) => emit('pageChange', current)

const handleSizeChange = (size: number) => emit('pageSizeChange', size)

const handleSelectionChange = (rows: T[]) => emit('selectionChange', rows)

const tableProps = props.tableProps ?? {}
</script>

<style scoped>
.page-table {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}
</style>
