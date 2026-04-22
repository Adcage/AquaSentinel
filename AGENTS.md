# AquaSentinel 项目指南

AquaSentinel 是水上安全监控系统，包含四个子项目：`backend`（Java/Spring Boot）、`frontend`（Vue 3 + TypeScript）、`yolo-service`（Python/Flask）、`android`（Kotlin/Jetpack Compose）。

---

## 构建、Lint 与测试命令

### Backend (Java / Spring Boot / Maven)

```bash
# 构建
cd backend && mvn compile

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=AlertRecordServiceImplTest

# 运行单个测试方法
mvn test -Dtest="AlertRecordServiceImplTest#testValidAlertRecord"
```

- Java 17，Spring Boot 3.2.3，MyBatis-Plus 3.5.5
- 无 checkstyle / PMD / Spotless 配置

### Frontend (Vue 3 / TypeScript / Vite)

```bash
cd frontend

# 开发服务器
npm run dev

# 构建
npm run build

# 运行全部单元测试
npm test

# 运行单个测试文件
npx vitest run src/tests/authService.test.ts

# 运行 E2E 测试
npm run test:e2e

# 类型检查（无独立脚本，手动执行）
npx vue-tsc --noEmit

# 重新生成 API 客户端代码（需后端运行）
npm run openapi
```

- Node >= 20.19 或 >= 22.12，Vite 7，Vitest 3，vue-tsc 类型检查
- 无 ESLint / Prettier 配置，格式不强制

### YOLO Service (Python / Flask)

```bash
cd yolo-service

# 安装依赖
pip install -r requirements.txt

# 运行全部测试
pytest

# 运行单个测试文件
pytest tests/test_drowning_rule_service.py

# 运行单个测试函数
pytest tests/test_drowning_rule_service.py::test_drowning_rule_requires_duration

# Lint（ruff 在 requirements.txt 中但无配置文件）
ruff check app/ tests/

# 启动开发服务器
python main.py --dev
```

- Python（无版本锁定），Flask 3.x，pytest，ruff（无 .ruff.toml / pyproject.toml 配置）

### Android (Kotlin / Jetpack Compose / Gradle)

```bash
cd android

# Debug 构建
./gradlew assembleDebug

# 运行单元测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.vision.swimsafe.RemoteMapperTest"

# 运行 Android Instrumented 测试
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

- compileSdk 36 / minSdk 29 / targetSdk 36，Kotlin，Compose BOM 2024.09.00
- `kotlin.code.style=official`，无 detekt / ktlint 配置

---

## 项目架构概览

| 子项目 | 技术栈 | 包/路径前缀 |
|--------|--------|-------------|
| backend | Spring Boot 3 + MyBatis-Plus + MySQL + JWT + WebSocket | `com.springboot` |
| frontend | Vue 3 (Composition API) + Pinia + Element Plus + ECharts + AMap | `src/` |
| yolo-service | Flask + Flask-Smorest + SQLAlchemy + YOLOv8 + DeepSort | `app/` |
| android | Kotlin + Jetpack Compose (Material 3) + Retrofit + OkHttp + AMap | `com.vision.swimsafe` |

---

## Backend 代码风格

- **包结构**：`controller / service / service.impl / mapper / model.dto / model.entity / model.vo / config / exception / security / utils / aop / websocket`
- **命名**：Controller `XxxController`、Service 接口 `XxxService`、实现 `XxxServiceImpl`、Mapper `XxxMapper`、DTO `[Entity][Action]Request`、VO `[Entity]VO`
- **路由命名**：kebab-case 复数，如 `@RequestMapping("/alert-records")`
- **实体字段**：snake_case 匹配数据库列（`@TableField(value = "alert_uid")`）；DTO/VO 字段用 camelCase
- **DI**：使用 `@Resource`（非 `@Autowired`）
- **错误处理**：`BusinessException` + `ThrowUtils.throwIf()` + `GlobalExceptionHandler`，统一返回 `BaseResponse<T>`
- **权限**：自定义 `@AuthCheck(mustRole = ...)` 注解 + AOP 拦截
- **注释**：Javadoc 和用户面向字符串均使用中文
- **缩进**：4 空格，K&R 大括号

## Frontend 代码风格

- **组件文件**：PascalCase 命名（`CameraGridCard.vue`），视图以 `View` 后缀（`LoginView.vue`）
- **TS 文件**：camelCase（`authService.ts`）
- **组合式 API**：全部使用 `<script setup lang="ts">`，无 Options API
- **Props**：`defineProps<Props>()` 配合 interface；Emits 用 `defineEmits<{...}>()`
- **导入顺序**：Vue 核心 → 第三方库 → `@/` 别名内部模块
- **API 三层架构**：`api/`（openapi2ts 自动生成）→ `services/`（业务门面）→ composables/views
- **类型**：自动生成类型在 `api/typings.d.ts`（`API` 命名空间）；手写业务类型在 `types/business.ts`
- **状态管理**：Pinia setup-function 风格，命名 `useXxxStore`
- **样式**：`<style scoped>`，CSS 自定义属性主题（`styles/theme.css`），类名 loose BEM
- **缩进**：2 空格；引号和分号风格不统一（自动生成文件用双引号+分号，手写文件倾向单引号无分号）

## YOLO Service 代码风格

- **函数**：snake_case；私有函数前缀 `_`
- **类**：PascalCase；模块级单例用 snake_case 实例（`ai_ws_push_service = AiWsPushService()`）
- **常量**：UPPER_SNAKE_CASE
- **类型注解**：全面使用，`from __future__ import annotations` 启用 `X | Y` 联合语法
- **错误处理**：`BusinessError(status_code, message)` + 三级错误处理器 + `success_payload/error_payload` 统一响应信封
- **API**：Flask-Smorest Blueprint + Marshmallow Schema 验证
- **注释**：关键逻辑内联注释和用户面向字符串使用中文；docstring 罕见
- **缩进**：4 空格

## Android 代码风格

- **包结构**：`data.remote / data.alert / data.stream / ui.screens / ui.components / ui.theme / ui.navigation / ui.model / config`
- **类/Composable**：PascalCase；对象单例用 `object`
- **函数**：普通函数 camelCase；Composable 函数 PascalCase
- **常量**：`private companion object` 内 `const val UPPER_SNAKE_CASE`
- **数据类**：API DTO 后缀 `Vo` / `Request`；UI 状态后缀 `UiState`
- **架构**：简化 MVVM（无 ViewModel），Compose 直接调用 `object` 仓库单例；`produceState` + `LaunchedEffect` 管理状态
- **主题**：Material 3 + 自定义 `AndroidTheme`、`Color.kt`、`Dimens.kt`、`Type.kt`
- **字符串**：全部中文
- **缩进**：4 空格，函数参数和枚举使用尾逗号

---

## 通用约定

- **语言**：所有用户面向文本、注释、Javadoc 和 log 使用中文
- **Emoji**：禁止在 UI 中使用 Emoji 表情符号
- **文档**：文档统一写到根目录的 docs/ 目录下面，按照文档对应的作用功能分文件夹存放，严禁放到其他的目录下面
- **视觉风格**：专业商务风，禁用大面积渐变，优先使用纯色和中性色
- **图标**：优先使用项目已集成的图标库（Element Plus Icons），自定义 SVG 必须封装为 `components/icons/` 下的独立组件
- **API 信封格式**：`{ code, data, message, requestId }`——后端 `code: 0` 表示成功（5 位数错误码），YOLO 服务 `code: "OK"` 表示成功
- **数据库约定**：`BIGINT AUTO_INCREMENT` 主键、`is_delete TINYINT DEFAULT 0` 软删除、`created_at / updated_at` 时间戳、snake_case 列名、中文表注释