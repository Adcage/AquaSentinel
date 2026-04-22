import { listCameraMaintenanceLogVoByPage } from "@/api/cameraMaintenanceController";
import { addRole, deleteRole, listRolePageVo } from "@/api/roleController";
import { exportCsv, exportExcel } from "@/api/statsController";
import { listSystemAuditLogVoByPage } from "@/api/systemAuditLogController";
import {
  unwrapApiData,
  normalizeDateTime,
  toApiDateTimeString,
} from "@/services/serviceUtils";

export interface MaintenanceRow {
  deviceName: string;
  content: string;
  operator: string;
  time: string;
}

export interface MaintenancePage {
  rows: MaintenanceRow[];
  total: number;
  current: number;
  pageSize: number;
}

export interface RoleTreeNode {
  id: string;
  label: string;
  children?: RoleTreeNode[];
}

export interface StatsExportRow {
  name: string;
  type: "CSV" | "Excel";
  operator: string;
  createdAt: string;
}

export interface SystemLogRow {
  time: string;
  operator: string;
  action: string;
  result: string;
}

export interface SystemLogPage {
  rows: SystemLogRow[];
  total: number;
  current: number;
  pageSize: number;
}

interface RoleGroup {
  id: string;
  label: string;
  sourceRoleCode: string;
}

const CORE_ROLE_GROUPS: RoleGroup[] = [
  {
    id: "ADMIN",
    label: "管理员",
    sourceRoleCode: "VENUE_ADMIN",
  },
  {
    id: "LIFEGUARD",
    label: "救生员",
    sourceRoleCode: "LIFEGUARD",
  },
];

export type CoreRoleKey = "ADMIN" | "LIFEGUARD";

export interface CoreRoleItem {
  key: CoreRoleKey;
  label: string;
  roleId?: number;
  roleCode: string;
  roleName: string;
  permissions: string[];
  status: number;
}

const parsePermissions = (permissionJson?: unknown) => {
  if (!permissionJson) {
    return [] as string[];
  }
  let data: unknown = permissionJson;
  if (typeof data === "string") {
    try {
      data = JSON.parse(data);
    } catch {
      return [] as string[];
    }
  }
  if (Array.isArray(data)) {
    return (data as unknown[]).map((item) => String(item)).filter(Boolean);
  }
  if (data && typeof data === "object") {
    const directPermissions = (data as Record<string, unknown>).permissions;
    if (Array.isArray(directPermissions)) {
      return directPermissions.map((item) => String(item)).filter(Boolean);
    }
  }
  return [] as string[];
};

const loadRoleMap = async () => {
  const response = await listRolePageVo({
    current: 1,
    pageSize: 100,
    status: 1,
  });
  const pageData = unwrapApiData<API.PageRoleVO>(response, "获取角色列表失败");
  const roleMap = new Map<string, API.RoleVO>();
  for (const role of pageData?.records ?? []) {
    const roleCode = String(role.roleCode ?? "").trim().toUpperCase();
    if (roleCode) {
      roleMap.set(roleCode, role);
    }
  }
  return roleMap;
};

export const getCoreRoleItems = async (): Promise<CoreRoleItem[]> => {
  const roleMap = await loadRoleMap();
  return CORE_ROLE_GROUPS.map((group) => {
    const sourceRole = roleMap.get(group.sourceRoleCode);
    const roleCode = sourceRole?.roleCode || group.sourceRoleCode;
    return {
      key: group.id as CoreRoleKey,
      label: group.label,
      roleId: sourceRole?.id,
      roleCode,
      roleName: sourceRole?.roleName || group.label,
      permissions: parsePermissions(sourceRole?.permissionJson),
      status: Number(sourceRole?.status ?? 1),
    };
  });
};


export const getDeviceMaintenancePage = async (
  query: { current?: number; pageSize?: number } = {},
): Promise<MaintenancePage> => {
  const response = await listCameraMaintenanceLogVoByPage({
    current: query.current ?? 1,
    pageSize: query.pageSize ?? 20,
    sortField: "maintained_at",
    sortOrder: "descend",
  });
  const pageData = unwrapApiData<API.PageCameraMaintenanceLogVO>(
    response,
    "获取维护记录失败",
  );
  const rows = (pageData?.records ?? []).map((item) => ({
    deviceName: `设备#${item.cameraId ?? "-"}`,
    content: item.maintenanceContent || "-",
    operator: item.maintainedBy || "-",
    time: normalizeDateTime(item.maintainedAt),
  }));

  return {
    rows,
    total: Number(pageData?.total ?? rows.length),
    current: Number(pageData?.current ?? query.current ?? 1),
    pageSize: Number(pageData?.size ?? query.pageSize ?? 20),
  };
};

export const getDeviceMaintenanceRows = async (): Promise<MaintenanceRow[]> => {
  const page = await getDeviceMaintenancePage();
  return page.rows;
};

export const getRolePermissionTree = async (): Promise<RoleTreeNode[]> => {
  const items = await getCoreRoleItems();
  return items.map((item) => ({
    id: item.key,
    label: item.label,
  }));
};

const getCoreRoleItem = async (roleKey: CoreRoleKey) => {
  const items = await getCoreRoleItems();
  const target = items.find((item) => item.key === roleKey);
  if (!target || !target.roleId) {
    throw new Error("未找到可操作的角色");
  }
  return target;
};

export const copyCoreRole = async (
  roleKey: CoreRoleKey,
  newRoleCode: string,
  newRoleName: string,
): Promise<number> => {
  const source = await getCoreRoleItem(roleKey);
  const response = await addRole({
    roleCode: newRoleCode,
    roleName: newRoleName,
    permissions: source.permissions,
    status: source.status,
  });
  return unwrapApiData<number>(response, "角色复制失败");
};

export const deleteCoreRole = async (roleKey: CoreRoleKey): Promise<boolean> => {
  const source = await getCoreRoleItem(roleKey);
  const response = await deleteRole({ id: source.roleId });
  return unwrapApiData<boolean>(response, "角色删除失败");
};

export const requestStatsExport = async (
  format: "csv" | "excel",
  payload: API.StatsExportRequest,
  operator: string,
): Promise<StatsExportRow> => {
  const response =
    format === "csv" ? await exportCsv(payload) : await exportExcel(payload);
  const data = unwrapApiData<Record<string, unknown>>(response, "导出报表失败");

  const rawDownloadUrl = String(data.downloadUrl ?? "").trim();
  if (!rawDownloadUrl) {
    throw new Error("导出地址缺失");
  }

  const apiBaseUrl = String(import.meta.env.VITE_API_BASE_URL || "/api").replace(
    /\/+$/,
    "",
  );
  const downloadUrl = /^https?:\/\//i.test(rawDownloadUrl)
    ? rawDownloadUrl
    : rawDownloadUrl.startsWith(`${apiBaseUrl}/`)
      ? rawDownloadUrl
      : rawDownloadUrl.startsWith("/")
        ? `${apiBaseUrl}${rawDownloadUrl}`
        : `${apiBaseUrl}/${rawDownloadUrl}`;

  const token = sessionStorage.getItem("token");
  const responseBlob = await fetch(downloadUrl, {
    method: "GET",
    credentials: "include",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!responseBlob.ok) {
    throw new Error("下载失败，请重新登录后重试");
  }

  const blob = await responseBlob.blob();
  const objectUrl = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = String(
    data.fileName ??
      `stats_export_${Date.now()}.${format === "csv" ? "csv" : "xlsx"}`,
  );
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(objectUrl);

  return {
    name: String(
      data.fileName ??
        `stats_export_${Date.now()}.${format === "csv" ? "csv" : "xlsx"}`,
    ),
    type: format === "csv" ? "CSV" : "Excel",
    operator,
    createdAt: normalizeDateTime(data.requestedAt),
  };
};

export const getSystemLogPage = async (
  query: API.SystemAuditLogQueryRequest,
): Promise<SystemLogPage> => {
  const response = await listSystemAuditLogVoByPage({
    current: query.current ?? 1,
    pageSize: query.pageSize ?? 20,
    sortField: "created_at",
    sortOrder: "descend",
    operatorName: query.operatorName,
    logCategory: query.logCategory,
    startCreatedAt: toApiDateTimeString(query.startCreatedAt),
    endCreatedAt: toApiDateTimeString(query.endCreatedAt),
  });
  const pageData = unwrapApiData<API.PageSystemAuditLogVO>(
    response,
    "获取系统日志失败",
  );

  const rows = (pageData?.records ?? []).map((item) => ({
    time: normalizeDateTime(item.createdAt),
    operator: item.operatorName || "-",
    action: item.requestUri || "-",
    result: item.responseCode && item.responseCode >= 400 ? "失败" : "成功",
  }));

  return {
    rows,
    total: Number(pageData?.total ?? rows.length),
    current: Number(pageData?.current ?? query.current ?? 1),
    pageSize: Number(pageData?.size ?? query.pageSize ?? 20),
  };
};

export const getSystemLogRows = async (
  query: API.SystemAuditLogQueryRequest,
): Promise<SystemLogRow[]> => {
  const page = await getSystemLogPage(query);
  return page.rows;
};
