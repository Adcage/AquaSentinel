<template>
  <div class="register-container">
    <div class="register-card">
      <div class="brand-strip">AI防溺水监测预警系统</div>
      <div class="register-header">
        <h2 class="title">账号登记示意页</h2>
        <p class="subtitle">用于创建后台账号，请按角色与验证码完成登记</p>
      </div>
      <el-form ref="formRef" :model="formState" :rules="rules" class="register-form" label-position="top">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formState.name" placeholder="请输入姓名" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="账号" prop="userAccount">
          <el-input v-model="formState.userAccount" placeholder="设置账号" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="formState.role" placeholder="请选择角色" size="large" style="width: 100%">
            <el-option label="场馆管理员" value="venue_admin" />
            <el-option label="超级管理员" value="super_admin" />
            <el-option label="普通查看员" value="viewer" />
          </el-select>
        </el-form-item>

        <el-form-item label="密码" prop="userPassword">
          <el-input
            v-model="formState.userPassword"
            type="password"
            show-password
            placeholder="设置密码"
            size="large"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="确认密码" prop="checkPassword">
          <el-input
            v-model="formState.checkPassword"
            type="password"
            show-password
            placeholder="确认密码"
            size="large"
          >
            <template #prefix>
              <el-icon><CircleCheck /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="图片验证码" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="formState.captcha" placeholder="请输入图片验证码" size="large" />
            <div class="captcha-image" aria-label="图片验证码">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image__img" />
              <span v-else>K9M3</span>
            </div>
            <el-button class="captcha-refresh" @click="refreshCaptcha">刷新验证码</el-button>
          </div>
        </el-form-item>

        <div class="page-note">
          <div class="page-note__title">页面定位</div>
          <div class="page-note__text">当前页面已接入真实注册接口，可作为后台账号开通入口。</div>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            class="register-button"
            :loading="loading"
            size="large"
            @click="onSubmit"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="login-link">
          已经有账号？<a @click="goToLogin">登录</a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { CircleCheck, Lock, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { fetchCaptcha, registerAccount } from '@/services/authService'

const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const captchaId = ref('')
const captchaImage = ref('')

const formState = reactive({
  name: '',
  userAccount: '',
  role: '',
  userPassword: '',
  checkPassword: '',
  captcha: '',
})

const validateCheckPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请确认密码'))
    return
  }

  if (value !== formState.userPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }

  callback()
}

const rules: FormRules<typeof formState> = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号不能少于4个字符', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码不能少于8个字符', trigger: 'blur' },
  ],
  checkPassword: [{ validator: validateCheckPassword, trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入图片验证码', trigger: 'blur' }],
}

const refreshCaptcha = async () => {
  try {
    const captcha = await fetchCaptcha()
    captchaId.value = captcha.captchaId
    captchaImage.value = captcha.imageDataUrl
    ElMessage.info('验证码图片已刷新')
  } catch (error) {
    captchaId.value = ''
    captchaImage.value = ''
    ElMessage.error((error as Error).message || '验证码加载失败')
  }
}

const onSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    if (!captchaId.value) {
      throw new Error('验证码已失效，请刷新后重试')
    }

    await registerAccount({
      displayName: formState.name,
      username: formState.userAccount,
      password: formState.userPassword,
      role: formState.role as 'super_admin' | 'venue_admin' | 'viewer',
      captchaId: captchaId.value,
      captchaCode: formState.captcha,
    })

    ElMessage.success('注册成功，请返回登录')
    router.push('/user/login')
  } catch (error) {
    ElMessage.error((error as Error).message || '注册失败')
    await refreshCaptcha()
    formState.captcha = ''
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/user/login')
}

onMounted(() => {
  void refreshCaptcha()
})
</script>

<style scoped>
.register-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg-page);
}

.register-card {
  width: 460px;
  padding: 0 40px 36px;
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border);
}

.brand-strip {
  margin: 0 -40px 28px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6f8fb;
  color: var(--color-primary);
  border-bottom: 1px solid var(--color-border);
  border-top-left-radius: 10px;
  border-top-right-radius: 10px;
  font-weight: 600;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: 8px;
}

.subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.register-form :deep(.el-input__wrapper) {
  padding: 8px 11px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 120px 104px;
  gap: 12px;
  width: 100%;
}

.captcha-image {
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: #f7f9fc;
  color: var(--color-primary-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 4px;
}

.captcha-image__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-refresh {
  height: 40px;
}

.page-note {
  border: 1px dashed var(--color-border);
  background: #fafcff;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 20px;
}

.page-note__title {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.page-note__text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-primary);
}

.register-button {
  width: 100%;
  height: 40px;
  border-radius: 4px;
  font-size: 15px;
}

.login-link {
  text-align: center;
  margin-top: 16px;
  color: var(--color-text-secondary);
}

.login-link a {
  color: var(--color-primary);
  cursor: pointer;
}

@media (max-width: 640px) {
  .register-card {
    width: 100%;
    margin: 0 16px;
    padding: 0 24px 24px;
  }

  .brand-strip {
    margin: 0 -24px 24px;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }
}
</style>
