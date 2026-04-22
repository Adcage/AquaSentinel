<template>
  <div class="profile-view admin-page">
    <div class="admin-page-header">
      <h1>个人中心</h1>
      <p>查看账户信息、编辑资料与维护登录安全</p>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="admin-table-card">
          <template #header>编辑资料</template>
          <el-form label-width="100px" class="profile-form">
            <el-form-item label="姓名">
              <el-input v-model="profile.name" />
            </el-form-item>
            <el-form-item label="联系方式">
              <el-input v-model="profile.phone" />
            </el-form-item>
            <el-form-item label="管理场馆">
              <el-input v-model="profile.venue" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存资料</el-button>
              <el-button @click="handleLogout">退出登录</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="admin-table-card">
          <template #header>修改密码</template>
          <el-form label-width="100px" class="profile-form">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="updatingPassword" @click="handleUpdatePassword">更新密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="admin-table-card">
      <template #header>最近登录记录</template>
      <el-table :data="loginRows" border>
        <el-table-column prop="time" label="登录时间" min-width="180" />
        <el-table-column prop="ip" label="IP地址" min-width="140" />
        <el-table-column prop="device" label="设备" min-width="180" />
      </el-table>
      <div class="login-pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="loginPagination.total"
          :current-page="loginPagination.current"
          :page-size="loginPagination.pageSize"
          @current-change="handleLoginPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { updateMyProfile } from '@/api/accessControlController'
import { listSystemAuditLogVoByPage } from '@/api/systemAuditLogController'
import { getUserVoById } from '@/api/userController'
import { getStoredAuthUser, logoutCurrentUser } from '@/services/authService'
import { normalizeDateTime, unwrapApiData } from '@/services/serviceUtils'
import router from '@/router'

const profile = reactive({
  name: '',
  phone: '',
  venue: '',
})

const savingProfile = ref(false)
const updatingPassword = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const loginRows = ref([
  { time: '-', ip: '-', device: '-' },
])

const loginPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const buildVenueLabel = (roles?: string[]) => {
  if (!roles || roles.length === 0) {
    return '未分配'
  }
  if (roles.includes('SUPER_ADMIN')) {
    return '全部场馆'
  }
  if (roles.includes('VENUE_ADMIN')) {
    return '所属场馆'
  }
  return '只读权限'
}

const loadProfile = async () => {
  const user = getStoredAuthUser()
  if (!user) {
    profile.name = '未登录用户'
    profile.phone = ''
    profile.venue = '未分配'
    return
  }

  profile.name = user.displayName || user.username || ''
  profile.venue = buildVenueLabel(user.roles)

  if (user.id) {
    try {
      const response = await getUserVoById({ id: user.id })
      const data = unwrapApiData<API.UserVO>(response, '加载用户信息失败')
      profile.phone = data?.phone || ''
      profile.name = data?.displayName || profile.name
    } catch {
      profile.phone = ''
    }
  }
}

const loadLoginRows = async (page = loginPagination.current) => {
  const user = getStoredAuthUser()
  if (!user?.id) {
    loginRows.value = []
    loginPagination.total = 0
    return
  }
  const response = await listSystemAuditLogVoByPage({
    current: page,
    pageSize: loginPagination.pageSize,
    operatorId: user.id,
    logCategory: 'LOGIN',
  })
  const pageData = unwrapApiData<API.PageSystemAuditLogVO>(response, '加载登录记录失败')
  loginRows.value = (pageData?.records ?? []).map((item) => ({
    time: normalizeDateTime(item.createdAt),
    ip: item.clientIp || '-',
    device: item.requestMethod && item.requestUri ? `${item.requestMethod} ${item.requestUri}` : '-',
  }))
  loginPagination.total = Number(pageData?.total ?? loginRows.value.length)
  loginPagination.current = Number(pageData?.current ?? page)
  loginPagination.pageSize = Number(pageData?.size ?? loginPagination.pageSize)
}

const handleLoginPageChange = (current: number) => {
  void loadLoginRows(current)
}

const handleSaveProfile = async () => {
  savingProfile.value = true
  try {
    const user = getStoredAuthUser()
    const response = await updateMyProfile({
      id: user?.id,
      displayName: profile.name,
      phone: profile.phone,
    })
    unwrapApiData<boolean>(response, '保存资料失败')
    ElMessage.success('资料已更新')
    if (user) {
      sessionStorage.setItem('authUser', JSON.stringify({ ...user, displayName: profile.name }))
    }
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  updatingPassword.value = true
  try {
    const user = getStoredAuthUser()
    const response = await updateMyProfile({
      id: user?.id,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    unwrapApiData<boolean>(response, '密码更新失败')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码已更新')
  } catch (error) {
    ElMessage.error((error as Error).message || '密码更新失败')
  } finally {
    updatingPassword.value = false
  }
}

const handleLogout = async () => {
  await logoutCurrentUser()
  ElMessage.success('已退出登录')
  router.push('/user/login')
}

onMounted(async () => {
  await loadProfile()
  await loadLoginRows()
})
</script>

<style scoped>
.profile-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-form {
  max-width: 520px;
}

.login-pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
