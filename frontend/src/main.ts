const _amapSec = import.meta.env.VITE_AMAP_SECURITY_KEY;
if (_amapSec) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (window as any)._AMapSecurityConfig = { securityJsCode: _amapSec };
}

import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import App from "./App.vue";
import router from "./router";

import "element-plus/dist/index.css";
import "./styles/theme.css";

const app = createApp(App);

const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus);

app.mount("#app");
