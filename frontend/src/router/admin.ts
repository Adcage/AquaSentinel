import type { RouteRecordRaw } from "vue-router";
import type { Component } from "vue";
import {
  Histogram,
  Monitor,
  Setting,
  User,
  UserFilled,
  Warning,
  ChatDotRound,
} from "@element-plus/icons-vue";
import BackendLayout from "@/layouts/BackendLayout.vue";

/** 菜单分组描述，挂在同组路由的 meta.group 上 */
export interface MenuGroup {
  key: string;
  title: string;
  icon: Component;
}

declare module "vue-router" {
  interface RouteMeta {
    title?: string;
    /** 顶级菜单图标（无分组时使用） */
    icon?: Component;
    /** 所属分组，有此字段则归入 sub-menu */
    group?: MenuGroup;
  }
}

const adminRoutes: RouteRecordRaw = {
  path: "/admin",
  component: BackendLayout,
  redirect: "/admin/dashboard",
  meta: { title: "管理后台" },
  children: [
    {
      path: "dashboard",
      name: "AdminDashboard",
      component: () => import("@/views/admin/dashboard/AdminDashboardView.vue"),
      meta: { title: "监控总览", icon: Monitor },
    },
    {
      path: "device",
      name: "DeviceManagement",
      component: () => import("@/views/admin/device/DeviceManagementView.vue"),
      meta: {
        title: "设备管理",
        group: { key: "business-group", title: "业务管理", icon: Warning },
      },
    },
    {
      path: "lifeguard",
      name: "LifeguardManagement",
      component: () =>
        import("@/views/admin/lifeguard/LifeguardManagementView.vue"),
      meta: {
        title: "救生员管理",
        group: { key: "business-group", title: "业务管理", icon: User },
      },
    },
    {
      path: "alarm",
      name: "AlarmManagement",
      component: () => import("@/views/admin/alarm/AlarmManagementView.vue"),
      meta: {
        title: "报警管理",
        group: { key: "business-group", title: "业务管理", icon: User },
      },
    },
    {
      path: "user",
      name: "UserManagement",
      component: () => import("@/views/admin/user/UserManagementView.vue"),
      meta: {
        title: "用户管理",
        group: { key: "business-group", title: "业务管理", icon: User },
      },
    },
    {
      path: "statistics",
      name: "Statistics",
      component: () => import("@/views/admin/statistics/StatisticsView.vue"),
      meta: {
        title: "统计分析",
        group: { key: "analysis-group", title: "统计分析", icon: Histogram },
      },
    },
    {
      path: "ai-chat",
      name: "AiChat",
      component: () => import("@/views/admin/aiChat/AiChatView.vue"),
      meta: {
        title: "AI 助手",
        group: { key: "intelligence-group", title: "智能分析", icon: ChatDotRound },
      },
    },
    {
      path: "settings",
      name: "SystemSettings",
      component: () => import("@/views/admin/settings/SystemSettingsView.vue"),
      meta: {
        title: "系统设置",
        group: { key: "settings-group", title: "系统设置", icon: Setting },
      },
    },
    {
      path: "profile",
      name: "Profile",
      component: () => import("@/views/admin/profile/ProfileView.vue"),
      meta: { title: "个人中心", icon: UserFilled },
    },
  ],
};

export default adminRoutes;
