<template>
  <div class="placeholder-view">
    <el-result
      status="404"
      title="页面不存在"
      sub-title="访问的地址无效，请返回首页继续浏览"
    >
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = (): void => {
  router.push('/');
};
</script>

<style scoped>
.placeholder-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
</style>

