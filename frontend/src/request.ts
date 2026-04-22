import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

const MAX_CONSECUTIVE_ERRORS = 3;
let consecutiveErrorCount = 0;
const myAxios = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
});

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    const token = sessionStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error);
  },
);

// 全局响应拦截器
myAxios.interceptors.response.use(
  async function (response) {
    consecutiveErrorCount = 0;
    return response;
  },
  function (error) {
    const status = error?.response?.status as number | undefined;
    const backendMessage = (
      error?.response?.data as { message?: string } | undefined
    )?.message;

    consecutiveErrorCount++;

    if (status === 401) {
      sessionStorage.removeItem("token");
      sessionStorage.removeItem("refreshToken");
      sessionStorage.removeItem("authUser");
      ElMessage.error("登录已过期，请重新登录");
      router.push("/user/login");
    } else if (status === 500 && consecutiveErrorCount <= MAX_CONSECUTIVE_ERRORS) {
      ElMessage.error(backendMessage || "服务器异常，请稍后重试");
    }

    const networkMessage = (() => {
      if (!error?.response) {
        const msg: string = error?.message ?? "";
        if (error?.code === "ECONNABORTED" || msg.includes("timeout")) {
          return "请求超时，请稍后重试";
        }
        return "网络连接失败，请检查网络";
      }
      return `请求失败（${status}）`;
    })();

    return Promise.reject(new Error(backendMessage || networkMessage));
  },
);

export default myAxios;
