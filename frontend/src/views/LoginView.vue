<template>
  <div class="login-container">
    <div class="login-shell">
      <section class="login-brand-panel">
        <LogoIcon :size="160" class="brand-logo" />
        <div class="brand-badge">管理端 · UI确认版</div>
        <h1>AI防溺水监测预警系统</h1>
        <p>面向场馆管理员与超级管理员，突出实时连接、报警优先与高效处置。</p>
        <ul class="brand-points">
          <li>实时监控大屏与报警响应联动</li>
          <li>设备、救生员、报警、统计一体化管理</li>
          <li>支持图片验证码、连接状态提示与统一权限入口</li>
        </ul>
      </section>

      <div class="login-card">
        <div class="login-header">
          <h2 class="title">管理员登录</h2>
          <p class="subtitle">请输入账号、密码和图片验证码</p>
        </div>
        <el-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          class="login-form"
          label-position="top"
        >
          <el-form-item label="管理员账号" prop="userAccount">
            <el-input
              v-model="formState.userAccount"
              placeholder="请输入管理员账号"
              size="large"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="登录密码" prop="userPassword">
            <el-input
              v-model="formState.userPassword"
              type="password"
              show-password
              placeholder="请输入登录密码"
              size="large"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="图片验证码" prop="captcha">
            <div class="captcha-row">
              <el-input
                v-model="formState.captcha"
                placeholder="请输入图片验证码"
                size="large"
                @keyup.enter="onSubmit"
              />
              <button
                type="button"
                class="captcha-image"
                aria-label="点击图片切换验证码"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="验证码"
                  class="captcha-image__img"
                />
                <span v-else>{{ captchaText }}</span>
              </button>
            </div>
            <div class="captcha-tip">点击图片切换验证码</div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="formState.remember">记住账号</el-checkbox>
            <el-button link class="forgot-btn">忘记密码</el-button>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              class="login-button"
              :loading="loading"
              size="large"
              @click="onSubmit"
            >
              登录管理端
            </el-button>
          </el-form-item>

          <div class="register-link">
            如需开通账号，请联系系统管理员统一配置。
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Lock, User } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { fetchCaptcha, loginAsAdmin } from "@/services/authService";
import LogoIcon from "@/components/icons/LogoIcon.vue";

const router = useRouter();
const loading = ref(false);
const formRef = ref<FormInstance>();
const captchaText = ref("AB7K");
const captchaId = ref("");
const captchaImage = ref("");

const formState = reactive({
  userAccount: "",
  userPassword: "",
  captcha: "",
  remember: true,
});

const rules: FormRules<typeof formState> = {
  userAccount: [{ required: true, message: "请输入账号", trigger: "blur" }],
  userPassword: [{ required: true, message: "请输入密码", trigger: "blur" }],
  captcha: [{ required: true, message: "请输入图片验证码", trigger: "blur" }],
};

const refreshCaptcha = async () => {
  try {
    const captcha = await fetchCaptcha();
    captchaId.value = captcha.captchaId;
    captchaImage.value = captcha.imageDataUrl;
    ElMessage.info("验证码图片已刷新");
  } catch (error) {
    captchaId.value = "";
    captchaImage.value = "";
    ElMessage.error((error as Error).message || "验证码加载失败");
  }
};

const onSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  loading.value = true;
  try {
    if (!captchaId.value) {
      throw new Error("验证码已失效，请刷新后重试");
    }
    const result = await loginAsAdmin({
      username: formState.userAccount,
      password: formState.userPassword,
      captchaId: captchaId.value,
      captchaCode: formState.captcha,
    });
    ElMessage.success(
      `登录成功，欢迎 ${result.user?.displayName || result.user?.username || "管理员"}`,
    );
    router.push("/admin/dashboard");
  } catch (error) {
    ElMessage.error((error as Error).message || "登录失败");
    await refreshCaptcha();
    formState.captcha = "";
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  void refreshCaptcha();
});
</script>

<style scoped>
.login-container {
  position: relative;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100dvh;
  background-color: #f0f2f5;
  background-image: radial-gradient(
    circle at 1px 1px,
    rgba(27, 79, 155, 0.08) 1px,
    transparent 0
  );
  background-size: 22px 22px;
  padding: 24px;
}

.login-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 460px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: #ffffff;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.login-brand-panel {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  background: linear-gradient(180deg, #001529 0%, #0b2a55 100%);
  color: #ffffff;
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
}

.login-brand-panel::before {
  content: "";
  position: absolute;
  top: 24px;
  right: 22px;
  width: 220px;
  height: 220px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 28px;
  transform: rotate(-10deg);
  z-index: -1;
}

.login-brand-panel::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(
      circle at 82% 18%,
      rgba(255, 255, 255, 0.12) 0,
      rgba(255, 255, 255, 0) 42%
    ),
    linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.06) 0,
      rgba(255, 255, 255, 0) 52%
    );
  z-index: -1;
}

.brand-logo {
  display: block;
  margin: 0 auto 16px;
  /* 极细白色边缘勾边，让 logo 从深蓝背景中清晰剥离 */
  filter: drop-shadow(0 0 0.8px rgba(255, 255, 255, 0.95))
    drop-shadow(0 0 1.5px rgba(255, 255, 255, 0.6))
    drop-shadow(0 2px 6px rgba(0, 0, 0, 0.25));
}

.brand-badge {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  letter-spacing: 0.5px;
}

.login-brand-panel h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.3;
  font-weight: 600;
}

.login-brand-panel p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.8;
}

.brand-points {
  margin: 0;
  padding-left: 18px;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.9;
}

.login-card {
  padding: 40px 40px 32px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: 8px;
}

.subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 8px 11px;
  min-height: 38px;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.captcha-row :deep(.el-input__wrapper) {
  height: 40px;
  min-height: 40px;
}

.captcha-image {
  height: 40px;
  width: 100%;
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
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.captcha-image__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-image:hover {
  border-color: #c9d8ef;
  background: #f0f5fd;
}

.captcha-image:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.captcha-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-button {
  width: 100%;
  height: 40px;
  border-radius: 4px;
  font-size: 15px;
}

.register-link {
  text-align: center;
  margin-top: 16px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-brand-panel {
    padding: 32px;
  }
}

@media (max-width: 640px) {
  .login-container {
    padding: 16px;
  }

  .login-card,
  .login-brand-panel {
    padding: 24px;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }
}
</style>
