<template>
  <div class="navbar">
    <div class="navbar-container">
      <div class="navbar-logo" @click="router.push('/')">通用系统模板</div>
      
      <nav class="navbar-menu">
        <a 
          v-for="item in menuItems" 
          :key="item.key"
          class="menu-item"
          :class="{ active: isActive(item.path) }"
          @click.prevent="handleMenuClick(item.path)"
        >
          {{ item.label }}
        </a>
      </nav>

      <div class="navbar-right">
        <el-space>
          <el-button link @click="router.push('/user/login')">登录示例</el-button>
          <el-button @click="router.push('/user/register')">注册示例</el-button>
        </el-space>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

interface MenuItem {
  key: string
  label: string
  path: string
}

const router = useRouter()
const route = useRoute()

const menuItems = ref<MenuItem[]>([
  { key: 'home', label: '首页', path: '/' },
  { key: 'admin', label: '后台模板', path: '/admin/dashboard' },
])

const isActive = (path: string): boolean => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const handleMenuClick = (path: string): void => {
  router.push(path)
}

</script>

<style scoped>
.navbar {
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 1000;
  border-bottom: 1px solid var(--color-border);
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  align-items: center;
  height: 64px;
}

.navbar-logo {
  font-size: 20px;
  font-weight: bold;
  color: var(--color-primary);
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  margin-right: 40px;
}

.navbar-menu {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-grow: 1;
  justify-content: center;
}

.menu-item {
  padding: 0 16px;
  height: 64px;
  line-height: 64px;
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
}

.menu-item:hover {
  color: var(--color-primary);
}

.menu-item.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 500;
}

.navbar-right {
  flex-shrink: 0;
}


@media (max-width: 768px) {
  .navbar-logo {
    margin-right: 10px;
    font-size: 16px;
  }
  .menu-item {
    padding: 0 8px;
    font-size: 13px;
  }
}
</style>
