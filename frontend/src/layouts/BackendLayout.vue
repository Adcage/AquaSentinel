<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sider">
      <div class="sider-brand" @click="router.push('/admin/dashboard')">
        <template v-if="collapsed">
          <span class="sider-brand__mini">AI</span>
        </template>
        <template v-else>
          <div class="sider-brand__title">AI防溺水监测预警系统</div>
          <div class="sider-brand__subtitle">综合管理平台</div>
        </template>
      </div>

      <div class="sider-toolbar">
        <el-button text class="collapse-button" @click="collapsed = !collapsed">
          <el-icon :size="18">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
        </el-button>
      </div>

      <el-menu
        :default-active="selectedKey"
        :default-openeds="openGroupKeys"
        :collapse="collapsed"
        class="menu"
        @select="handleMenuSelect"
      >
        <template v-for="item in menuItems" :key="item.key">
          <el-sub-menu v-if="item.children" :index="item.key">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.key"
              :index="child.key"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="item.key">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="main-container" direction="vertical">
      <el-header class="header">
        <div class="header-content">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>管理后台</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <div class="ws-status" :class="{ connected: isSocketConnected }">
              <span class="dot" />
              <span>{{ isSocketConnected ? "实时连接" : "连接断开" }}</span>
            </div>
            <el-divider direction="vertical" />
            <el-button text class="icon-button" @click="toggleAiChat">
              <el-icon :size="18"><ChatDotRound /></el-icon>
            </el-button>
            <el-divider direction="vertical" />
            <el-badge :value="pendingAlarmCount" :max="99" class="alarm-badge">
              <el-button
                text
                class="icon-button"
                @click="router.push({ name: 'AlarmManagement' })"
              >
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-divider direction="vertical" />
            <el-dropdown>
              <div class="user-entry">
                <el-icon><User /></el-icon>
                <span>{{ currentUserName }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push({ name: 'Profile' })"
                    >个人中心</el-dropdown-item
                  >
                  <el-dropdown-item>修改密码</el-dropdown-item>
                  <el-dropdown-item
                    @click="router.push({ name: 'SystemSettings' })"
                    >系统设置</el-dropdown-item
                  >
                  <el-dropdown-item divided @click="handleLogout"
                    >退出登录</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-main class="content-layout">
        <router-view />
      </el-main>
    </el-container>

    <AiChatPanel
      v-if="showAiChat"
      class="ai-chat-sidebar"
      :show-close="true"
      @close="showAiChat = false"
    />
  </el-container>
</template>

<script setup lang="ts">
import { Bell, Expand, Fold, User, ChatDotRound } from "@element-plus/icons-vue";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { Component } from "vue";
import type { MenuGroup } from "@/router/admin";
import { getDashboardMetrics } from "@/services/dashboardService";
import { getStoredAuthUser, logoutCurrentUser } from "@/services/authService";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  alertWsService,
  type WsConnectionStatus,
} from "@/services/alertWsService";
import AiChatPanel from "@/components/business/AiChatPanel.vue";

interface MenuItem {
  key: string;
  title: string;
  icon?: Component;
  children?: MenuItem[];
}

const router = useRouter();
const route = useRoute();

const collapsed = ref(false);
const isSocketConnected = ref(true);
const pendingAlarmCount = ref(0);
const currentUserName = ref("系统管理员");
const isAlertDialogVisible = ref(false);
const pendingAlertQueue = ref<Record<string, unknown>[]>([]);
const showAiChat = ref(false);

const menuItems = computed((): MenuItem[] => {
  const adminRoute = router
    .getRoutes()
    .find((record) => record.path === "/admin");
  const groups = new Map<string, MenuItem>();
  const items: MenuItem[] = [];

  for (const child of adminRoute?.children ?? []) {
    const { title, icon, group } = child.meta ?? {};
    const key = child.name as string;

    if (group) {
      const {
        key: groupKey,
        title: groupTitle,
        icon: groupIcon,
      } = group as MenuGroup;

      if (!groups.has(groupKey)) {
        const groupItem: MenuItem = {
          key: groupKey,
          title: groupTitle,
          icon: groupIcon,
          children: [],
        };
        groups.set(groupKey, groupItem);
        items.push(groupItem);
      }

      groups.get(groupKey)?.children?.push({ key, title: title ?? key });
      continue;
    }

    items.push({ key, title: title ?? key, icon: icon as Component });
  }

  return items;
});

const selectedKey = computed(() => (route.name ? String(route.name) : ""));

const openGroupKeys = computed(() =>
  menuItems.value
    .filter((item) => item.children?.length)
    .map((item) => item.key),
);

const handleMenuSelect = (key: string) => {
  router.push({ name: key });
};

const currentPageTitle = computed(() => String(route.meta.title ?? ""));

const EVENT_TYPE_TEXT: Record<string, string> = {
  DROWNING: "溺水",
  DROWING: "溺水",
  OFF_POST: "脱岗",
  DEVICE_OFFLINE: "设备离线",
};

const RISK_LEVEL_TEXT: Record<string, string> = {
  HIGH: "高",
  MEDIUM: "中",
  LOW: "低",
};

const RULE_HIT_TEXT: Record<string, string> = {
  posture_abnormal: "姿态异常",
  thermal_abnormal: "温感异常",
  duration_abnormal: "持续时长异常",
};

const toEventTypeText = (value: string) => {
  const key = String(value || "").trim().toUpperCase();
  return EVENT_TYPE_TEXT[key] || (key ? `事件(${key})` : "未知事件");
};

const toRiskLevelText = (value: string) => {
  const key = String(value || "").trim().toUpperCase();
  return RISK_LEVEL_TEXT[key] || (key ? `等级(${key})` : "未知");
};

const toRuleHitText = (value: string) => {
  const key = String(value || "").trim();
  return RULE_HIT_TEXT[key] || key;
};

const showNextAlertDialog = () => {
  if (isAlertDialogVisible.value || pendingAlertQueue.value.length === 0) {
    return;
  }
  const nextPayload = pendingAlertQueue.value.shift();
  if (!nextPayload) {
    return;
  }
  isAlertDialogVisible.value = true;

  const alertId = Number(nextPayload.alertId ?? 0);
  const eventType = String(nextPayload.eventType ?? "DROWNING");
  const cameraId = Number(nextPayload.cameraId ?? 0);
  const riskLevel = String(nextPayload.riskLevel ?? "MEDIUM").toUpperCase();
  const durationSecRaw = Number(nextPayload.durationSec ?? 0);
  const durationText =
    Number.isFinite(durationSecRaw) && durationSecRaw > 0
      ? `${durationSecRaw.toFixed(1)}秒`
      : "-";
  const ruleHits = Array.isArray(nextPayload.ruleHits)
    ? nextPayload.ruleHits
        .map((item) => String(item ?? "").trim())
        .filter((item) => item.length > 0)
    : [];
  const ruleText =
    ruleHits.length > 0
      ? ruleHits.map((item) => toRuleHitText(item)).join(" / ")
      : "未提供";

  const navigateToMonitor = async () => {
    const query: Record<string, string> = {
      fromAlert: "1",
      alertTs: String(Date.now()),
    };
    if (cameraId > 0) {
      query.focusCameraId = String(cameraId);
      query.fullscreenCameraId = String(cameraId);
    }
    await router.push({ name: "AdminDashboard", query });
  };

  ElMessageBox.confirm(
    `检测到${toEventTypeText(eventType)}风险\n风险等级：${toRiskLevelText(
      riskLevel,
    )}\n持续时长：${durationText}\n触发规则：${ruleText}`,
    "紧急告警",
    {
      type: "error",
      center: true,
      closeOnClickModal: false,
      closeOnPressEscape: false,
      confirmButtonText: "前往监控并全屏",
      cancelButtonText: alertId > 0 ? "转到报警详情" : "稍后处理",
      customClass: "drowning-alert-dialog",
      distinguishCancelAndClose: true,
    },
  )
    .then(async () => {
      isAlertDialogVisible.value = false;
      await navigateToMonitor();
      showNextAlertDialog();
    })
    .catch(async (action: string) => {
      isAlertDialogVisible.value = false;
      if (action !== "cancel") {
        showNextAlertDialog();
        return;
      }
      if (alertId > 0) {
        await router.push({
          name: "AlarmManagement",
          query: { alertId: String(alertId) },
        });
      }
      showNextAlertDialog();
    });
};

const handleAlertPayload = (payload: Record<string, unknown>) => {
  pendingAlarmCount.value += 1;

  if (isAlertDialogVisible.value) {
    pendingAlertQueue.value.push(payload);
    return;
  }

  pendingAlertQueue.value.push(payload);
  showNextAlertDialog();
};

const dispatchRealtimeBatchEvent = (payload: Record<string, unknown>) => {
  window.dispatchEvent(
    new CustomEvent("monitor-realtime-ws-batch", {
      detail: payload,
    }),
  );
};

const dispatchVideoFrameEvent = (
  cameraId: number,
  blob: Blob,
  frameHeader?: import("@/services/alertWsService").WsVideoFramePayload,
) => {
  if (!cameraId || cameraId <= 0 || !(blob instanceof Blob)) {
    return;
  }
  window.dispatchEvent(
    new CustomEvent("monitor-video-frame", {
      detail: {
        cameraId,
        blob,
        frameHeader,
      },
    }),
  );
};

const dispatchWsStatusChangedEvent = (status: WsConnectionStatus) => {
  window.dispatchEvent(
    new CustomEvent("alert-ws-status-changed", {
      detail: {
        status,
      },
    }),
  );
};

const handleLogout = async () => {
  await logoutCurrentUser();
  ElMessage.success("已退出登录");
  router.push("/user/login");
};

const toggleAiChat = () => {
  showAiChat.value = !showAiChat.value;
};

onMounted(async () => {
  const user = getStoredAuthUser();
  currentUserName.value = user?.displayName || user?.username || "系统管理员";
  const token = sessionStorage.getItem("token") || "";
  alertWsService.connect(
    token,
    (payload) => {
      if (payload.messageType === "ALERT_CREATED") {
        const data =
          payload.data && typeof payload.data === "object"
            ? (payload.data as Record<string, unknown>)
            : {};
        handleAlertPayload(data);
        return;
      }
      if (payload.messageType === "MONITOR_REALTIME_BATCH") {
        const data =
          payload.data && typeof payload.data === "object"
            ? (payload.data as Record<string, unknown>)
            : {};
        dispatchRealtimeBatchEvent(data);
        return;
      }
      if (payload.messageType === "MONITOR_REALTIME_HEARTBEAT") {
        dispatchRealtimeBatchEvent({});
      }
    },
    (status) => {
      isSocketConnected.value = status === "connected";
      dispatchWsStatusChangedEvent(status);
    },
    (cameraId, blob, frameHeader) => {
      dispatchVideoFrameEvent(cameraId, blob, frameHeader);
    },
  );
  try {
    const metrics = await getDashboardMetrics();
    pendingAlarmCount.value = metrics.pendingAlarmCount;
  } catch {
    pendingAlarmCount.value = 0;
  }
});

onUnmounted(() => {
  alertWsService.disconnect();
});
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.main-container {
  overflow: hidden;
  flex: 1;
  min-width: 0;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  height: var(--header-height);
  border-bottom: 1px solid var(--color-border);
  padding: 0;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-left :deep(.el-breadcrumb__inner) {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-left :deep(.el-breadcrumb__separator) {
  margin: 0 10px;
}

.header-left :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.ws-status .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger);
}

.ws-status.connected .dot {
  background: var(--color-success);
}

.alarm-badge {
  margin: 0 8px;
}

.icon-button {
  color: var(--color-text-secondary);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--color-text-secondary);
}

.sider {
  background: var(--color-sidebar-bg);
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  transition: width 0.2s;
  height: 100%;
  overflow-y: auto;
}

.sider-brand {
  height: 96px;
  padding: 16px;
  box-sizing: border-box;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  cursor: pointer;
}

.sider-brand__title {
  font-size: 18px;
  line-height: 26px;
  font-weight: 700;
  color: #41a0ff;
}

.sider-brand__subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.sider-brand__mini {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: rgba(65, 160, 255, 0.18);
  color: #41a0ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.sider-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.collapse-button {
  width: 100%;
  height: 100%;
  border-radius: 0;
  color: rgba(255, 255, 255, 0.75);
}

.menu {
  border-right: none;
  background: transparent;
}

.content-layout {
  background: var(--color-bg-page);
  padding: 24px;
  overflow-y: auto;
  height: 100%;
}

.page-breadcrumb {
  margin-bottom: 12px;
  color: var(--color-text-tertiary);
}

.admin-layout :deep(.admin-page) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.admin-layout :deep(.admin-page-header h1) {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.admin-layout :deep(.admin-page-header p) {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.admin-layout :deep(.admin-filter-card),
.admin-layout :deep(.admin-table-card) {
  border: 1px solid var(--color-border);
}

.admin-layout :deep(.admin-filter-card .el-card__body) {
  padding: 16px 24px 12px;
}

.admin-layout :deep(.admin-table-card .el-card__body) {
  padding: 12px 16px 16px;
}

.admin-layout :deep(.admin-toolbar) {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.admin-layout :deep(.admin-round-btn) {
  border-radius: 18px;
  padding: 0 18px;
}

.admin-layout :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #595959;
  font-weight: 600;
}

.admin-layout :deep(.el-table .el-table__cell) {
  padding-top: 12px;
  padding-bottom: 12px;
}

.admin-layout :deep(.el-menu) {
  border-right: none;
}

.admin-layout :deep(.el-menu-item),
.admin-layout :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.75);
  height: 48px;
}

.admin-layout :deep(.el-menu-item:hover),
.admin-layout :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.admin-layout :deep(.el-menu-item.is-active) {
  color: #fff;
  background: var(--color-primary);
  position: relative;
}

.admin-layout :deep(.el-menu-item.is-active::before) {
  content: "";
  position: absolute;
  left: 0;
  top: 10px;
  width: 3px;
  height: 28px;
  background: #fff;
  border-radius: 0 2px 2px 0;
}

.admin-layout :deep(.el-sub-menu .el-menu) {
  background: var(--color-sidebar-sub-bg);
}

.admin-layout :deep(.drowning-alert-dialog) {
  border: 1px solid #ffccc7;
  border-radius: 14px;
  overflow: hidden;
}

.admin-layout :deep(.drowning-alert-dialog .el-message-box__header) {
  background: linear-gradient(90deg, #fff1f0 0%, #fff 100%);
}

.admin-layout :deep(.drowning-alert-dialog .el-message-box__title) {
  color: #cf1322;
  font-weight: 700;
}

.admin-layout :deep(.drowning-alert-dialog .el-message-box__message p) {
  color: #3f3f46;
  line-height: 1.8;
  white-space: pre-line;
}

.admin-layout :deep(.drowning-alert-dialog .el-button--primary) {
  background: #cf1322;
  border-color: #cf1322;
}

.ai-chat-sidebar {
  position: fixed;
  right: 24px;
  top: 64px;
  bottom: 24px;
  width: 420px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  display: flex;
  flex-direction: column;
}
</style>
